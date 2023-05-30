package ca.gc.aafc.dina.filter;

import java.util.Arrays;
import java.util.List;

import lombok.Getter;

@Getter
public class FilterGroup implements FilterComponent {
  enum Conjunction {
    /**
     * All expressions must be true for a result to be included.
     */
    AND, 
    
    /**
     * At least one expression must be true for a result to be included.
     */
    OR
  }

  /**
   * Conjunction currently set for the expressions.
   */
  private Conjunction conjunction;

  /**
   * All of the filter expressions for this group. Groups can also be nested here so you can have
   * groups inside groups.
   */
  private List<FilterComponent> expressions;

  /**
   * Sets the conjunction to 'AND' and adds the provided filter components to the list of expressions.
   *
   * @param components the filter components to be added.
   */
  public void and(FilterComponent... components) {
    conjunction = Conjunction.AND;
    expressions.addAll(Arrays.asList(components));
  }

  /**
  * Sets the conjunction to 'OR' and adds the provided filter components to the list of expressions.
  *
  * @param components the filter components to be added.
  */
  public void or(FilterComponent... components) {
    conjunction = Conjunction.OR;
    expressions.addAll(Arrays.asList(components));
  }

  /**
   * Checks if the list of filter expressions contains one or more items.
   *
   * @return {@code true} if the list contains one or more items, {@code false} otherwise.
   */
  public boolean hasExpressions() {
    return expressions != null && !expressions.isEmpty();
  }
}
