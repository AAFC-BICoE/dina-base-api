package ca.gc.aafc.dina.filter;

import java.util.List;
import java.util.function.Predicate;

/**
 * Utility class for transforming {@link FilterComponent} instances within a {@link QueryComponent}.
 *
 * <p>Recursively searches and replaces filter components based on a predicate condition,
 * maintaining immutability by creating new instances.</p>
 *
 * <pre>{@code
 * QueryComponent result = FilterTransformer.transform(
 *   originalQuery,
 *   component -> component instanceof FilterExpression expr && "userId".equals(expr.attribute()),
 *   component -> FilterGroup.builder().component(...).build()
 * );
 * }</pre>
 */
public final class FilterComponentMutator {

  private FilterComponentMutator() {
    // utility
  }

  /**
   * Mutates filters within a QueryComponent that match the given predicate.
   * All other QueryComponent properties are preserved unchanged.
   *
   * @param queryComponent the query component containing filters to transform
   * @param predicate condition to test each filter component
   * @param mutation function to apply to matching components
   * @return a new QueryComponent with transformed filters
   */
  public static QueryComponent mutate(QueryComponent queryComponent,
                                      Predicate<FilterComponent> predicate,
                                      FilterComponentMutation mutation) {
    if (queryComponent.getFilters() == null) {
      return queryComponent;
    }

    FilterComponent transformedFilters = transformComponent(
      queryComponent.getFilters(),
      predicate,
      mutation
    );

    return QueryComponent.builder()
      .filters(transformedFilters)
      .fiql(queryComponent.getFiql())
      .includes(queryComponent.getIncludes())
      .sorts(queryComponent.getSorts())
      .fields(queryComponent.getFields())
      .optionalFields(queryComponent.getOptionalFields())
      .pageOffset(queryComponent.getPageOffset())
      .pageLimit(queryComponent.getPageLimit())
      .build();
  }

  private static FilterComponent transformComponent(FilterComponent component,
                                                    Predicate<FilterComponent> predicate,
                                                    FilterComponentMutation mutation) {
    // Check if this component matches the predicate
    if (predicate.test(component)) {
      return mutation.apply(component);
    }

    // If it's a FilterGroup, recursively transform its children
    if (component instanceof FilterGroup group) {
      List<FilterComponent> transformedComponents = group.getComponents().stream()
        .map(c -> transformComponent(c, predicate, mutation))
        .toList();

      return FilterGroup.builder()
        .conjunction(group.getConjunction())
        .components(transformedComponents)
        .build();
    }

    // No transformation
    return component;
  }

  @FunctionalInterface
  public interface FilterComponentMutation {
    FilterComponent apply(FilterComponent component);
  }
}
