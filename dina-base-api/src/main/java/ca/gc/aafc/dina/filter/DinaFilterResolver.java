package ca.gc.aafc.dina.filter;

import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.exception.UnknownAttributeException;
import ca.gc.aafc.dina.mapper.DinaMappingRegistry;
import com.github.tennaito.rsql.jpa.JpaPredicateVisitor;
import com.github.tennaito.rsql.misc.ArgumentParser;
import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.ast.Node;
import io.crnk.core.engine.internal.utils.PropertyUtils;
import io.crnk.core.queryspec.AbstractPathSpec;
import io.crnk.core.queryspec.Direction;
import io.crnk.core.queryspec.FilterSpec;
import io.crnk.core.queryspec.PathSpec;
import io.crnk.core.queryspec.QuerySpec;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;

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
 *   <ul>
 *    <li>Resolving filter adapter</li>
 *    <li>Generating JPA predicates for filtering</li>
 *    <li>Generating JPA Order by's for filtering</li>
 *  </ul>
 * </pre>
 */
@Log4j2
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
   * Performs LEFT JOIN fetches to join the related entities specified in the QuerySpec's sort attributes.
   *
   * This method is used to perform LEFT JOIN fetches on related entities in a Root object, based on
   * the sort attributes specified in a QuerySpec object.
   *
   * @param root       The Root object to which the related entities are to be joined.
   * @param querySpec  The QuerySpec object containing the sorting information.
   * @param registry   The DinaMappingRegistry used for mapping information.
   */
  public static <E extends DinaEntity> void leftJoinSortRelations(Root<E> root, @NonNull QuerySpec querySpec,
    @NonNull DinaMappingRegistry registry) {

    if (root == null || querySpec.getSort().isEmpty()) {
      return;
    }

    // Remove the last element from the sort attribute path since we want to only include the nested
    // relationship. Ex. "department.name" would retrieve "department".
    List<List<String>> relationsToJoin = new ArrayList<>();
    querySpec.getSort().stream().map(AbstractPathSpec::getAttributePath).forEach(sort -> relationsToJoin
        .add(parseMappableRelationshipPath(registry, querySpec.getResourceClass(),
            sort.size() <= 1 ? sort
                : sort.subList(0, sort.size() - 1))));

    relationsToJoin.forEach(relation -> joinAttributePath(root, relation));
  }

  /**
   * Extracts the included attribute paths from the given QuerySpec as a Set.
   *
   * This method extracts the included attribute paths from the provided QuerySpec and returns
   * them as a Set of Strings. The included attribute paths represent chains of relationships between
   * entities, using dot notation. For example, "departments.employees" represents a join between the
   * Department entity and the Employee entity via the "employees" attribute.
   *
   * @param querySpec The QuerySpec object from which the included attribute paths are extracted.
   * @return A Set containing the included attribute paths in the QuerySpec.
   *         The attribute paths are represented as chains of relationship names separated by dots.
   *         If no attribute paths are included in the QuerySpec, an empty Set is returned.
   */
  public static Set<String> extractIncludesSet(@NonNull QuerySpec querySpec) {
    // getIncludedRelations never returns null
    if (querySpec.getIncludedRelations().isEmpty() ) {
      return Set.of();
    }

    Set<String> includes = new HashSet<>();
    querySpec.getIncludedRelations().forEach(ir ->
        includes.add(String.join(".", ir.getAttributePath())));
    return includes;
  }
  
  /**
   * Extracts relationships from the QuerySpec and ensures their validity.
   *
   * This method extracts the relationships (attribute paths) from the provided QuerySpec and validates
   * them using the given DinaMappingRegistry. The relationships are represented as chains of relationship
   * names separated by dots, and nested relationships will be broken up to individual components.
   * For example, "departments.employees" represents a join between the Department entity and the Employee
   * entity via the "employees" attribute.
   *
   * @param querySpec The QuerySpec object from which the relationships are extracted and validated.
   * @param registry  The DinaMappingRegistry used for mapping information to validate the relationships.
   * @return A Set containing the extracted relationships from the QuerySpec.
   *         The relationships are represented as valid chains of relationship names separated by dots.
   *         If no relationships are present in the QuerySpec, an empty Set is returned.
   */
  public static Set<String> extractRelationships(@NonNull QuerySpec querySpec, @NonNull DinaMappingRegistry registry) {
    // getIncludedRelations never returns null
    if (querySpec.getIncludedRelations().isEmpty()) {
      return Set.of();
    }

    Set<String> relationsToJoin = new HashSet<>();
    querySpec.getIncludedRelations().forEach(ir -> {
      List<String> attributePath = ir.getAttributePath();
      if (!attributePath.isEmpty()) {
        List<String> mappablePath = parseMappableRelationshipPath(registry, querySpec.getResourceClass(), attributePath);
        if (!mappablePath.isEmpty()) {
          relationsToJoin.add(String.join(".", mappablePath));
        }
      }
    });
    return relationsToJoin;
  }

  /**
   * Parses an attribute path starting from the given resourceClass, searching for mappable relationships.
   * 
   * This method iterates through each part of the attribute path and checks if the relationship can be found
   * within the given resourceClass. If a part of the path cannot be found, an empty list is returned.
   * 
   * External relationships and relationships that cannot be found will be ignored.
   * 
   * @param registry      The DinaMappingRegistry used for mapping information.
   * @param resourceClass The resourceClass from where to start the search for the attribute.
   * @param attributePath The attribute path to parse, e.g., ("department", "employees") -> department.employees.
   * @return A List containing mappable attribute paths in the order they appear in attributePath.
   */
  private static List<String> parseMappableRelationshipPath(
      @NonNull DinaMappingRegistry registry,
      @NonNull Class<?> resourceClass,
      @NonNull List<String> attributePath) {
    List<String> fullPath = new ArrayList<>(attributePath.size());
    Class<?> dtoClass = resourceClass;

    for (String attr : attributePath) {
      if (hasMappableRelation(registry, dtoClass, attr)) {
        fullPath.add(attr);
        dtoClass = PropertyUtils.getPropertyClass(dtoClass, attr);
      } else {
        // Internal relationship not found. Return an empty path.
        log.debug(
          "The attribute path '{}' cannot be found on the '{}' resource. Please ensure that the attribute is present on the resource and it's not pointing to an external relationship. The include will be ignored.",
          () -> String.join(".", attributePath), resourceClass::getCanonicalName);
        return List.of();
      }
    }
    return fullPath;
  }

  /**
   * Checks if a given class has a specified relation.
   *
   * This method determines whether the provided class has a relation with the specified name.
   * It uses the DinaMappingRegistry to find mappable relations for the given class and compares the
   * relation names in a case-insensitive manner with the provided relation name.
   *
   * @param registry The DinaMappingRegistry used for finding mappable relations.
   * @param aClass   The class to be evaluated for the specified relation.
   * @param relation The name of the relation to be checked in the class.
   *
   * @return {@code true} if the class has the specified relation; otherwise, {@code false}.
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

    restrictions.add(SimpleFilterHandler.getRestriction(
      root, cb, rsqlArgumentParser::parse, em.getMetamodel(), querySpec.getFilters()));

    // RSQL Filters
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

  /**
   * Joins multiple attribute paths in a Root object using LEFT JOIN fetches.
   *
   * This method is used to join multiple attribute paths in a Root object using LEFT JOIN fetches.
   * The attribute paths are provided as a List of Strings, and the method performs successive LEFT JOIN
   * fetches for each attribute path in the list.
   *
   * @param root          The Root object to which the attribute paths are to be joined.
   * @param attributePath A List of Strings representing the attribute paths to be joined.
   */
  private static void joinAttributePath(Root<?> root, List<String> attributePath) {
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
