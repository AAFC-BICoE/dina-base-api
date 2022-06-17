package ca.gc.aafc.dina.filter;

import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.exception.UnknownAttributeException;
import ca.gc.aafc.dina.mapper.DinaMappingRegistry;
import com.github.tennaito.rsql.jpa.JpaPredicateVisitor;
import com.github.tennaito.rsql.misc.ArgumentParser;
import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.ast.Node;
import io.crnk.core.engine.internal.utils.PropertyUtils;
import io.crnk.core.queryspec.Direction;
import io.crnk.core.queryspec.FilterSpec;
import io.crnk.core.queryspec.PathSpec;
import io.crnk.core.queryspec.QuerySpec;
import lombok.NonNull;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.FetchParent;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * DinaFilterResolver handles the responsibilities for dina repo filtering operations. Those responsibilities
 * are the following.
 * <pre>
 *    <ul>
 *    <li>Resolving filter adapter</li>
 *    <li>Generating JPA predicates for filtering</li>
 *    <li>Generating JPA Order by's for filtering</li>
 *  </ul>
 * </pre>
 */
public class DinaFilterResolver {

  private final JpaPredicateVisitor<Object> visitor = new JpaPredicateVisitor<>();
  private final RSQLParser rsqlParser = new RSQLParser();
  private final RsqlFilterAdapter rsqlFilterAdapter;
  private final ArgumentParser rsqlArgumentParser = new DinaFilterArgumentParser();

  public DinaFilterResolver(RsqlFilterAdapter rsqlFilterAdapter) {
    this.rsqlFilterAdapter = rsqlFilterAdapter;
    this.visitor.getBuilderTools().setArgumentParser(rsqlArgumentParser);
  }

  /**
   * Returns a new List of filter specs resolved from the given filters for fields being mapped by field
   * adapters. Filters for fields that are not resolved through field adapters will remain in the new list,
   * Filter for fields that are resolved through field adapters will be replaced by the adapters {@link
   * ca.gc.aafc.dina.mapper.DinaFieldAdapter#toFilterSpec}
   *
   * @param resource - Type of resource to be filtered.
   * @param filters  - Filter specs to resolve.
   * @param registry - Registry used for resolution.
   * @return a new List of filter specs resolved from the given filters
   */
  public static List<FilterSpec> resolveFilterAdapters(
    @NonNull Class<?> resource,
    @NonNull List<FilterSpec> filters,
    @NonNull DinaMappingRegistry registry
  ) {
    List<FilterSpec> newFilters = new ArrayList<>();
    for (FilterSpec filterSpec : filters) {
      List<String> path = filterSpec.getAttributePath();
      // find nested resource class
      Class<?> dtoClass = registry.resolveNestedResourceFromPath(resource, path);

      if (CollectionUtils.isNotEmpty(path) && registry.findFieldAdapterForClass(dtoClass).isPresent()) {
        registry.findFieldAdapterForClass(dtoClass).get().findFilterSpec(path.get(path.size() - 1))
          .ifPresentOrElse(
            specs -> newFilters.addAll(resolveSpecs(filterSpec, specs)),
            () -> newFilters.add(filterSpec));
      } else {
        newFilters.add(filterSpec);
      }
    }
    return newFilters;
  }

  /**
   * Convenience method to return a list of filter specs resolved from a given Filter Spec mapping function.
   *
   * @param applyValue - filter spec to apply
   * @param specs      - Functions to invoke apply.
   * @return a list of resolved filter specs.
   */
  private static List<FilterSpec> resolveSpecs(
    @NonNull FilterSpec applyValue,
    @NonNull Function<FilterSpec, FilterSpec[]> specs
  ) {
    List<String> path = applyValue.getAttributePath();
    List<String> pathPrefix = new ArrayList<>(path.subList(0, path.size() - 1));
    return Stream.of(specs.apply(applyValue))
      .map(fs -> {
        // Resolve filter spec path with generated spec paths
        List<String> newPath = Stream
          .concat(pathPrefix.stream(), fs.getAttributePath().stream())
          .collect(Collectors.toList());
        return PathSpec.of(newPath).filter(fs.getOperator(), fs.getValue());
      })
      .collect(Collectors.toList());
  }

  /**
   * Convenience method to left join relations that have been parsed from a given querySpec using a mapping
   * registry.
   *
   * @param <E>       root entity type
   * @param root      root path of entity
   * @param querySpec query spec to parse
   * @param registry  registry to use to determine relations
   */
  public static <E extends DinaEntity> void leftJoinSortRelations(Root<E> root, @NonNull QuerySpec querySpec,
    @NonNull DinaMappingRegistry registry) {

    if (root == null || querySpec.getSort().isEmpty()) {
      return;
    }

    List<Set<String>> relationsToJoin = new ArrayList<>();
    querySpec.getSort().forEach(sort ->
            relationsToJoin.add(parseMappablePaths(registry, querySpec.getResourceClass(), sort.getAttributePath())));

    relationsToJoin.forEach(relation -> {
        joinAttributePath(root, relation);
    });
  }

  /**
   * Extracts relationships from the query specs and make sure they are valid.
   * @param querySpec
   * @param registry
   * @return Set with all the relationships or an empty set. Never null;
   */
  public static Set<String> extractRelationships(@NonNull QuerySpec querySpec, @NonNull DinaMappingRegistry registry) {
    // getIncludedRelations never returns null
    if( querySpec.getIncludedRelations().isEmpty() ) {
      return Set.of();
    }

    Set<String> relationsToJoin = new HashSet<>();
    querySpec.getIncludedRelations().forEach(ir ->
            relationsToJoin.addAll(parseMappablePaths(registry, querySpec.getResourceClass(), ir.getAttributePath())));
    return relationsToJoin;
  }

  /**
   *
   * @param registry
   * @param resourceClass
   * @param attributePath
   * @return never null
   */
  private static Set<String> parseMappablePaths(
          @NonNull DinaMappingRegistry registry,
          @NonNull Class<?> resourceClass,
          @NonNull List<String> attributePath) {
    Set<String> relationPaths = new HashSet<>();
    Class<?> dtoClass = resourceClass;
    for (String attr : attributePath) {
      if (hasMappableRelation(registry, dtoClass, attr)) {
        relationPaths.add(attr);
        dtoClass = PropertyUtils.getPropertyClass(dtoClass, attr);
      } else {
        break;
      }
    }
    return relationPaths;
  }

//  private static void parseMappablePaths(
//    @NonNull DinaMappingRegistry registry,
//    @NonNull Class<?> resourceClass,
//    @NonNull Set<PathSpec> mappablePaths,
//    @NonNull List<String> attributePath
//  ) {
//    List<String> relationPath = new ArrayList<>();
//    Class<?> dtoClass = resourceClass;
//    for (String attr : attributePath) {
//      if (hasMappableRelation(registry, dtoClass, attr)) {
//        relationPath.add(attr);
//        dtoClass = PropertyUtils.getPropertyClass(dtoClass, attr);
//      } else {
//        break;
//      }
//    }
//    if (CollectionUtils.isNotEmpty(relationPath)) {
//      mappablePaths.add(PathSpec.of(relationPath));
//    }
//  }

  /**
   * Returns true if a given class has a given relation.
   *
   * @param registry registry to determine results
   * @param aClass   class to evaluate
   * @param relation relation to evaluate
   * @return true if a given class has a given relation.
   */
  private static boolean hasMappableRelation(DinaMappingRegistry registry, Class<?> aClass, String relation) {
    return registry.findMappableRelationsForClass(aClass)
      .stream().anyMatch(rel -> rel.getName().equalsIgnoreCase(relation));
  }

  /**
   * Returns an array of predicates by mapping crnk filters into JPA restrictions with a given querySpec,
   * criteria builder, root, ids, and id field name.
   *
   * @param <E>         - root entity type
   * @param querySpec   - crnk query spec with filters, cannot be null
   * @param cb          - the criteria builder, cannot be null
   * @param root        - the root type, cannot be null
   * @param ids         - collection of ids, can be null
   * @param idFieldName - collection of ids, can be null if collections is null, else throws null pointer.
   * @throws UnknownAttributeException if an attribute used in the {@link QuerySpec} rsql filter is unknown
   * @return - array of predicates
   */
  public <E> Predicate[] buildPredicates(
    @NonNull QuerySpec querySpec,
    @NonNull CriteriaBuilder cb,
    @NonNull Root<E> root,
    Collection<Serializable> ids,
    String idFieldName,
    @NonNull EntityManager em
  ) throws UnknownAttributeException {
    final List<Predicate> restrictions = new ArrayList<>();

    //Simple Filters
    restrictions.add(SimpleFilterHandler.getRestriction(
      root, cb, rsqlArgumentParser::parse, em.getMetamodel(), querySpec.getFilters()));
    //Rsql Filters
    Optional<FilterSpec> rsql = querySpec.findFilter(PathSpec.of("rsql"));
    if (rsql.isPresent() && StringUtils.isNotBlank(rsql.get().getValue())) {
      try {
        visitor.defineRoot(root);
        final Node rsqlNode = processRsqlAdapters(rsqlFilterAdapter, rsqlParser.parse(rsql.get().getValue()));
        restrictions.add(rsqlNode.accept(visitor, em));
      } catch (IllegalArgumentException iaEx) {
        //  if attribute of the given name does not exist
        throw new UnknownAttributeException(iaEx);
      }
    } else {
      restrictions.add(cb.and());
    }

    if (CollectionUtils.isNotEmpty(ids)) {
      Objects.requireNonNull(idFieldName);
      restrictions.add(root.get(idFieldName).in(ids));
    }

    return restrictions.toArray(Predicate[]::new);
  }

  /**
   * Parses a crnk {@link QuerySpec} to return a list of {@link Order} from a given {@link CriteriaBuilder}
   * and {@link Path}.
   *
   * @param <T>  - root type
   * @param qs   - crnk query spec to parse
   * @param cb   - criteria builder to build orders
   * @param root - root path of entity
   * @param caseSensitive - Should order by on text fields be case sensitive or no ?
   * @return a list of {@link Order} from a given {@link CriteriaBuilder} and {@link Path}
   * @throws UnknownAttributeException if an attribute used in the {@link QuerySpec} sort is unknown
   */
  public static <T> List<Order> getOrders(QuerySpec qs, CriteriaBuilder cb, Path<T> root, boolean caseSensitive)
      throws UnknownAttributeException {
    return qs.getSort().stream().map(sort -> {
      Expression<?> orderByExpression;
      Path<T> from = root;

      try {
        for (String path : sort.getAttributePath()) {
          from = from.get(path);
        }
      } catch (IllegalArgumentException iaEx) {
        //  if attribute of the given name does not exist
        throw new UnknownAttributeException(iaEx);
      }

      if (!caseSensitive && from.getJavaType() == String.class) {
        orderByExpression = cb.lower(from.as(String.class));
      } else {
        orderByExpression = from;
      }

      return sort.getDirection() == Direction.ASC ? cb.asc(orderByExpression) : cb.desc(orderByExpression);
    }).collect(Collectors.toList());
  }

  private static void joinAttributePath(Root<?> root, Set<String> attributePath) {
    if (root == null || CollectionUtils.isEmpty(attributePath)) {
      return;
    }
    FetchParent<?, ?> join = root;
    for (String path : attributePath) {
      join = join.fetch(path, JoinType.LEFT);
    }
  }

  /**
   * Returns a Rsql node processed with a given adapter.
   *
   * @param adapter adapter to process
   * @param node    node to process
   * @return a processed rsql node
   */
  private static Node processRsqlAdapters(RsqlFilterAdapter adapter, Node node) {
    Node rsqlNode = node;
    if (adapter != null && node != null) {
      rsqlNode = adapter.process(rsqlNode);
    }
    return rsqlNode;
  }

}
