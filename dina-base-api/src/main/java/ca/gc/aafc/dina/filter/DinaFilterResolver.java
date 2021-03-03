package ca.gc.aafc.dina.filter;

import ca.gc.aafc.dina.jpa.BaseDAO;
import ca.gc.aafc.dina.mapper.DinaMappingRegistry;
import com.github.tennaito.rsql.jpa.JpaPredicateVisitor;
import com.github.tennaito.rsql.misc.ArgumentParser;
import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.ast.Node;
import io.crnk.core.queryspec.Direction;
import io.crnk.core.queryspec.FilterSpec;
import io.crnk.core.queryspec.IncludeRelationSpec;
import io.crnk.core.queryspec.PathSpec;
import io.crnk.core.queryspec.QuerySpec;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.FetchParent;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Component used to map crnk filters into valid JPA objects.
 */
@Component
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class DinaFilterResolver {

  public static final JpaPredicateVisitor<Object> VISITOR = new JpaPredicateVisitor<>();
  private final SimpleFilterHandler simpleFilterHandler;
  private final BaseDAO baseDAO;
  private final ArgumentParser rsqlArgumentParser;
  private final RSQLParser rsqlParser = new RSQLParser();
  private final Map<Class<?>, RsqlFilterAdapter> rsqlAdapterPerClass = new HashMap<>();

  /**
   * Returns a new List of filter specs resolved from the given filters for fields being mapped by
   * field adapters. Filters for fields that are not resolved through field adapters will remain in
   * the new list, Filter for fields that are resolved through field adapters will be replaced by
   * the adapters {@link ca.gc.aafc.dina.mapper.DinaFieldAdapter#toFilterSpec}
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

      if (CollectionUtils.isNotEmpty(path) && registry.getFieldAdaptersPerClass().containsKey(dtoClass)) {
        registry.getFieldAdaptersPerClass().get(dtoClass).findFilterSpec(path.get(path.size() - 1))
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
   * Convenience method to return a list of filter specs resolved from a given Filter Spec mapping
   * function.
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
   * Returns an array of predicates by mapping crnk filters into JPA restrictions
   * with a given querySpec, criteria builder, root, ids, and id field name.
   *
   * @param <E>
   *                      - root entity type
   * @param querySpec
   *                      - crnk query spec with filters, cannot be null
   * @param cb
   *                      - the criteria builder, cannot be null
   * @param root
   *                      - the root type, cannot be null
   * @param ids
   *                      - collection of ids, can be null
   * @param idFieldName
   *                      - collection of ids, can be null if collections is null,
   *                      else throws null pointer.
   * @return - array of predicates
   */
  public <E> Predicate[] buildPredicates(
    @NonNull QuerySpec querySpec,
    @NonNull CriteriaBuilder cb,
    @NonNull Root<E> root,
    Collection<Serializable> ids,
    String idFieldName
  ) {
    final List<Predicate> restrictions = new ArrayList<>();

    //Simple Filters
    restrictions.add(simpleFilterHandler.getRestriction(querySpec, root, cb));

    handleRsqlFilters(querySpec, cb, root, restrictions);

    if (CollectionUtils.isNotEmpty(ids)) {
      Objects.requireNonNull(idFieldName);
      restrictions.add(root.get(idFieldName).in(ids));
    }

    return restrictions.toArray(Predicate[]::new);
  }

  private <E> void handleRsqlFilters(
    QuerySpec querySpec,
    CriteriaBuilder cb,
    Root<E> root,
    List<Predicate> restrictions
  ) {
    Optional<FilterSpec> rsql = querySpec.findFilter(PathSpec.of("rsql"));
    if (rsql.isPresent() && StringUtils.isNotBlank(rsql.get().getValue())) {
      VISITOR.defineRoot(root).getBuilderTools().setArgumentParser(rsqlArgumentParser);
      RsqlFilterAdapter adapter = rsqlAdapterPerClass.get(querySpec.getResourceClass());
      final Node rsqlNode = processRsqlAdapters(adapter, rsqlParser.parse(rsql.get().getValue()));
      restrictions.add(baseDAO.createWithEntityManager(em -> rsqlNode.accept(VISITOR, em)));
    } else {
      restrictions.add(cb.and());
    }
  }

  /**
   * Parses a crnk {@link QuerySpec} to return a list of {@link Order} from a
   * given {@link CriteriaBuilder} and {@link Path}.
   *
   * @param <T>
   *               - root type
   * @param qs
   *               - crnk query spec to parse
   * @param cb
   *               - critera builder to build orders
   * @param root
   *               - root path of entity
   * @return a list of {@link Order} from a given {@link CriteriaBuilder} and {@link Path}
   */
  public static <T> List<Order> getOrders(QuerySpec qs, CriteriaBuilder cb, Path<T> root) {
    return qs.getSort().stream().map(sort -> {
      Path<T> from = root;
      for (String path : sort.getAttributePath()) {
        from = from.get(path);
      }
      return sort.getDirection() == Direction.ASC ? cb.asc(from) : cb.desc(from);
    }).collect(Collectors.toList());
  }

  /**
   * Adds left joins for eager Loading the relationships of a given query spec to a given root.
   *
   * @param root      - root path to add joins
   * @param includedRelations - relations to map
   */
  public static void eagerLoadRelations(Root<?> root, List<IncludeRelationSpec> includedRelations) {
    for (IncludeRelationSpec relation : includedRelations) {
      FetchParent<?, ?> join = root;
      for (String path : relation.getAttributePath()) {
        join = join.fetch(path, JoinType.LEFT);
      }
    }
  }

  /**
   * Process a Rsql node processed with a given adapter.
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

  public void addRsqlAdapter(Class<?> clz, RsqlFilterAdapter adapter) {
    if (clz != null && adapter != null) {
      rsqlAdapterPerClass.put(clz, adapter);
    }
  }

  public void clearRsqlAdaptersForClass(Class<?> clz) {
    if (clz != null) {
      rsqlAdapterPerClass.remove(clz);
    }
  }

}
