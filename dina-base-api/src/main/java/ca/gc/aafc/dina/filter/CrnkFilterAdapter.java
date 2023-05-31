package ca.gc.aafc.dina.filter;

import com.querydsl.core.types.Ops;

import ca.gc.aafc.dina.filter.FilterGroup.Conjunction;
import ca.gc.aafc.dina.filter.FilterGroup.FilterGroupBuilder;
import io.crnk.core.queryspec.FilterOperator;
import io.crnk.core.queryspec.FilterSpec;

/**
 * Converts Crnk based filters into Dina Filter Components.
 */
public final class CrnkFilterAdapter {
  private CrnkFilterAdapter() {
  }

  /**
   * Converts a Crnk {@link FilterSpec} to a {@link FilterComponent}.
   *
   * @param filterSpec the filter spec to be converted
   * @return the equivalent filter component representation
   */
  public static FilterComponent convertFilterSpecToComponent(FilterSpec filterSpec) {
    if (filterSpec == null) {
      return null;
    }

    // Check if this level contains a singular expression or it's expressing a group
    // of expressions.
    if (filterSpec.getExpression() == null || filterSpec.getExpression().isEmpty()) {
      // Singular expression found, convert into a FilterExpression.
      FilterExpression filter = new FilterExpression(
          filterSpec.getAttributePath().get(0),
          convertCrnkOperatorToFilterExpressionOperator(filterSpec.getOperator()),
          filterSpec.getValue());

      return filter;
    } else {
      // Group of expressions found, convert into a FilterGroup.
      FilterGroupBuilder groupBuilder = FilterGroup.builder();
      for (FilterSpec subFilterSpec : filterSpec.getExpression()) {
        groupBuilder.add(convertFilterSpecToComponent(subFilterSpec));
      }

      // Check the type of conjunction being performed on the group (AND/OR)
      groupBuilder.conjunction(convertCrnkOperatorToFilterGroupConjunction(filterSpec.getOperator()));

      return groupBuilder.build();
    }
  }

  /**
   * Converts supported crnk operators into supported filter expression operators.
   * 
   * @param crnkOperator Crnk operator.
   * @return Filter expression operator.
   */
  public static Ops convertCrnkOperatorToFilterExpressionOperator(FilterOperator crnkOperator) {
    return switch (crnkOperator.toString()) {
      case "EQ" -> Ops.EQ;
      case "NEQ" -> Ops.NE;
      case "LIKE" -> Ops.LIKE;
      case "LT" -> Ops.LT;
      case "LE" -> Ops.LOE;
      case "GT" -> Ops.GT;
      case "GE" -> Ops.GOE;
      default -> throw new IllegalArgumentException("Unsupported CRNK operator to convert: " + crnkOperator.toString());
    };
  }

  /**
   * Converts support crnk conjunctions (these are combined with the operators in
   * crnk) into
   * supported filter group conjunctions.
   * 
   * @param crnkOperator Crnk operator (Conjunction are defined here for crnk).
   * @return Conjunction enum converted from the crnk operator.
   */
  public static Conjunction convertCrnkOperatorToFilterGroupConjunction(FilterOperator crnkOperator) {
    return switch (crnkOperator.toString()) {
      case "AND" -> Conjunction.AND;
      case "OR" -> Conjunction.OR;
      default -> throw new IllegalArgumentException("Unsupported CRNK conjunction operator to convert: " + crnkOperator);
    };
  }
}
