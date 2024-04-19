package ca.gc.aafc.dina.filter;

import java.util.List;
import java.util.Optional;
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
  private final List<String> includes;
  private final List<String> sorts;

  private final Integer pageOffset;
  private final Integer pageLimit;

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
