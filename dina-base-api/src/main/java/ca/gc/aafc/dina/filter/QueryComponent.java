package ca.gc.aafc.dina.filter;

import java.util.List;
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

}
