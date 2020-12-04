package ca.gc.aafc.dina.filter;

import ca.gc.aafc.dina.mapper.DinaMappingRegistry;
import io.crnk.core.queryspec.Direction;
import io.crnk.core.queryspec.FilterSpec;
import io.crnk.core.queryspec.IncludeRelationSpec;
import io.crnk.core.queryspec.QuerySpec;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
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
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Component used to map crnk filters into valid JPA objects.
 */
@Component
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class DinaFilterResolver {

  private final SimpleFilterHandler simpleFilterHandler;
  private final RsqlFilterHandler rsqlFilterHandler;

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
  public static List<FilterSpec> resolveFilterSpecs(
    @NonNull Class<?> resource,
    @NonNull List<FilterSpec> filters,
    @NonNull DinaMappingRegistry registry
  ) {
    List<FilterSpec> newFilters = new ArrayList<>();
    for (FilterSpec filterSpec : filters) {
      List<String> attributePath = filterSpec.getAttributePath();
      Class<?> dtoClass = registry.findDeeplyNestedResource(resource, attributePath);

      // find last attribute in path
      String attr = attributePath.stream().reduce((s, s2) -> s2)
        .orElseThrow(() -> new IllegalArgumentException("Query spec must provide an attribute path"));

      if (registry.getFieldAdaptersPerClass().containsKey(dtoClass)) {
        registry.getFieldAdaptersPerClass().get(dtoClass).findFilterSpec(attr)
          .ifPresentOrElse(
            specs -> newFilters.addAll(List.of(specs.apply(filterSpec.getValue()))),
            () -> newFilters.add(filterSpec));
      }
    }
    return newFilters;
  }

  /**
   * Returns an array of predicates by mapping crnk filters into JPA restrictions with a given
   * querySpec, criteria builder, root, ids, and id field name.
   *
   * @param <E>         - root entity type
   * @param querySpec   - crnk query spec with filters, cannot be null
   * @param cb          - the criteria builder, cannot be null
   * @param root        - the root type, cannot be null
   * @param ids         - collection of ids, can be null
   * @param idFieldName - collection of ids, can be null if collections is null, else throws null
   *                    pointer.
   * @return - array of predicates
   */
  public <E> Predicate[] buildPredicates(
    @NonNull QuerySpec querySpec,
    @NonNull CriteriaBuilder cb,
    @NonNull Root<E> root,
    Collection<Serializable> ids,
    String idFieldName
  ) {
    List<Predicate> restrictions = new ArrayList<>();
    restrictions.add(simpleFilterHandler.getRestriction(querySpec, root, cb));
    restrictions.add(rsqlFilterHandler.getRestriction(querySpec, root, cb));

    if (CollectionUtils.isNotEmpty(ids)) {
      Objects.requireNonNull(idFieldName);
      restrictions.add(root.get(idFieldName).in(ids));
    }

    return restrictions.toArray(Predicate[]::new);
  }

  /**
   * Parses a crnk {@link QuerySpec} to return a list of {@link Order} from a given {@link
   * CriteriaBuilder} and {@link Path}.
   *
   * @param <T>  - root type
   * @param qs   - crnk query spec to parse
   * @param cb   - critera builder to build orders
   * @param root - root path of entity
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
   * @param root              - root path to add joins
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

}
