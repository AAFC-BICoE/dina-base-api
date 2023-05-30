package ca.gc.aafc.dina.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.querydsl.core.types.Ops;

import ca.gc.aafc.dina.filter.FilterGroup.Conjunction;
import io.crnk.core.queryspec.FilterOperator;
import io.crnk.core.queryspec.FilterSpec;

public class CrnkFilterAdapterTest {

  @Test
  void convertCrnkFilterSpec_andFilter_generatesEquivalentFilterGroup() {
    // GET /person?filter[firstName][EQ]=John&filter[lastName][EQ]=Doe
    FilterSpec firstNameFilter = new FilterSpec(List.of("firstName"), FilterOperator.EQ, "John");
    FilterSpec lastNameFilter = new FilterSpec(List.of("lastName"), FilterOperator.EQ, "Doe");
    FilterSpec andFilter = new FilterSpec(FilterOperator.AND, List.of(firstNameFilter, lastNameFilter));

    // Convert to a filter component.
    FilterGroup component = (FilterGroup) CrnkFilterAdapter.convertFilterSpecToComponent(andFilter);

    // Check the FilterGroup to ensure it's "AND" and 2 expressions are found within it.
    assertEquals(Conjunction.AND, component.getConjunction());
    assertEquals(2, component.getExpressions().size());

    FilterExpression firstNameExpression = (FilterExpression) component.getExpressions().get(0);
    FilterExpression lastNameExpression = (FilterExpression) component.getExpressions().get(1);

    // Verify the first name and last name.
    assertEquals("firstName", firstNameExpression.getAttribute());
    assertEquals(Ops.EQ, firstNameExpression.getOperator());
    assertEquals("John", firstNameExpression.getValue());

    assertEquals("lastName", lastNameExpression.getAttribute());
    assertEquals(Ops.EQ, lastNameExpression.getOperator());
    assertEquals("Doe", lastNameExpression.getValue());
  }

  @Test
  void convertCrnkFilterSpec_simpleFilter_generatesEquivalentFilterGroup() {
    // GET /person?filter[firstName][EQ]=Jane
    FilterSpec simpleFilter = new FilterSpec(List.of("firstName"), FilterOperator.EQ, "Jane");

    // Convert to a filter expression.
    FilterExpression filterGenerated = (FilterExpression) CrnkFilterAdapter.convertFilterSpecToComponent(simpleFilter);

    // Assert that it generates an equivalent filter expression from the crnk filter spec.
    assertEquals("firstName", filterGenerated.getAttribute());
    assertEquals(Ops.EQ, filterGenerated.getOperator());
    assertEquals("Jane", filterGenerated.getValue());
  }
}
