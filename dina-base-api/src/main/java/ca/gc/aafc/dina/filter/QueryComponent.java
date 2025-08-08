package ca.gc.aafc.dina.filter;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.Builder;
import lombok.Getter;

/**
 * Models the query part sent to the API.
 * Includes: filters, sorts, includes and page
 */
@Builder
@Getter
public class QueryComponent {

  private final FilterComponent filters;
  private final String fiql;
  private final Set<String> includes;
  // must be an ordered list
  private final List<String> sorts;

  // Sparse fields set where the key is the type
  private final Map<String, List<String>> fields;

  private final Integer pageOffset;
  private final Integer pageLimit;

  // Represents an empty query component
  public static final QueryComponent EMPTY = QueryComponent.builder().build();

  /**
   * Tries to get the filters as {@link FilterGroup} if possible.
   * @return
   */
  public Optional<FilterGroup> getFilterGroup() {
    if (filters instanceof FilterGroup filtersHasGroup) {
      return Optional.of(filtersHasGroup);
    }
    return Optional.empty();
  }

  /**
   * Tries to get the filters as {@link FilterExpression} if possible.
   * @return
   */
  public Optional<FilterExpression> getFilterExpression() {
    if (filters instanceof FilterExpression filterHasExpression) {
      return Optional.of(filterHasExpression);
    }
    return Optional.empty();
  }
}
