package ca.gc.aafc.dina.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.querydsl.core.types.Ops;

import ca.gc.aafc.dina.filter.FilterGroup.Conjunction;
import io.crnk.core.queryspec.FilterOperator;
import io.crnk.core.queryspec.FilterSpec;

public class CrnkFilterAdapterTest {

  /**
   * Complex filter conversion test that converts multiple groups and expressions.
   * 
   * QuerySpec being converted:
   * <pre>
   * (firstName == "John" AND lastName == "Doe") OR (firstName == "Jane" AND lastName == "Smith")
   * </pre>
   * 
   * Which should generate the following structure:
   * 
   * <pre>
   * new FilterGroup().or(
   *   new FilterGroup().and(
   *     new FilterExpression("firstName", EQ, "John"), 
   *     new FilterExpression("lastName", EQ, "Doe")
   *   ), 
   *   new FilterGroup().and(
   *     new FilterExpression("firstName", EQ, "Jane"), 
   *     new FilterExpression("lastName", EQ, "Smith")
   *   )
   * )
   * </pre>
   */
  @Test
  void convertCrnkFilterSpec_complexFilter_generatesEquivalentFilterGroup() {
    // Create the FilterSpec for the first condition: (firstName == "John" AND
    // lastName == "Doe")
    FilterSpec firstNameFilter1 = new FilterSpec(List.of("firstName"), FilterOperator.EQ, "John");
    FilterSpec lastNameFilter1 = new FilterSpec(List.of("lastName"), FilterOperator.EQ, "Doe");
    FilterSpec andFilter1 = new FilterSpec(FilterOperator.AND, List.of(firstNameFilter1, lastNameFilter1));

    // Create the FilterSpec for the second condition: (firstName == "Jane" AND
    // lastName == "Smith")
    FilterSpec firstNameFilter2 = new FilterSpec(List.of("firstName"), FilterOperator.EQ, "Jane");
    FilterSpec lastNameFilter2 = new FilterSpec(List.of("lastName"), FilterOperator.EQ, "Smith");
    FilterSpec andFilter2 = new FilterSpec(FilterOperator.AND, List.of(firstNameFilter2, lastNameFilter2));

    // Create the FilterSpec for the final condition: (condition1 OR condition2)
    FilterSpec entireFilter = new FilterSpec(FilterOperator.OR, List.of(andFilter1, andFilter2));

    // Convert to a filter component.
    FilterGroup component = (FilterGroup) CrnkFilterAdapter.convertFilterSpecToComponent(entireFilter);

    // Assert it's the equivalent structure.
    assertEquals(Conjunction.OR, component.getConjunction(), "Top level conjunction doesn't match.");
    assertEquals(2, component.getExpressions().size(), "Expected 2 top level conditions (andFilter1 and andFilter2)");

    // Verify the firstNameFilter1 and lastNameFilter1 part.
    FilterGroup andFilterGroup1 = (FilterGroup) component.getExpressions().get(0);
    assertEquals(Conjunction.AND, andFilterGroup1.getConjunction(), "The firstName and lastName must both match.");
    assertEquals(2, andFilterGroup1.getExpressions().size(), "Expected the firstName and lastName filter conditions.");

    FilterExpression firstNameExpression1 = (FilterExpression) andFilterGroup1.getExpressions().get(0);
    FilterExpression lastNameExpression1 = (FilterExpression) andFilterGroup1.getExpressions().get(1);

    // Verify the first name and last name.
    assertEquals("firstName", firstNameExpression1.getAttribute());
    assertEquals(Ops.EQ, firstNameExpression1.getOperator());
    assertEquals("John", firstNameExpression1.getValue());

    assertEquals("lastName", lastNameExpression1.getAttribute());
    assertEquals(Ops.EQ, lastNameExpression1.getOperator());
    assertEquals("Doe", lastNameExpression1.getValue());

    // Verify the firstNameFilter2 and lastNameFilter2 part.
    FilterGroup andFilterGroup2 = (FilterGroup) component.getExpressions().get(1);
    assertEquals(Conjunction.AND, andFilterGroup2.getConjunction(), "The firstName and lastName must both match.");
    assertEquals(2, andFilterGroup2.getExpressions().size(), "Expected the firstName and lastName filter conditions.");

    FilterExpression firstNameExpression2 = (FilterExpression) andFilterGroup2.getExpressions().get(0);
    FilterExpression lastNameExpression2 = (FilterExpression) andFilterGroup2.getExpressions().get(1);

    // Verify the first name and last name.
    assertEquals("firstName", firstNameExpression2.getAttribute());
    assertEquals(Ops.EQ, firstNameExpression2.getOperator());
    assertEquals("Jane", firstNameExpression2.getValue());

    assertEquals("lastName", lastNameExpression2.getAttribute());
    assertEquals(Ops.EQ, lastNameExpression2.getOperator());
    assertEquals("Smith", lastNameExpression2.getValue());
  }

  @Test
  void convertCrnkFilterSpec_simpleFilter_generatesEquivalentFilterGroup() {
    FilterSpec simpleFilter = new FilterSpec(List.of("firstName"), FilterOperator.EQ, "Jane");

    // Convert to a filter expression.
    FilterExpression filterGenerated = (FilterExpression) CrnkFilterAdapter.convertFilterSpecToComponent(simpleFilter);

    // Assert that it generates an equivalent filter expression from the crnk filter spec.
    assertEquals("firstName", filterGenerated.getAttribute());
    assertEquals(Ops.EQ, filterGenerated.getOperator());
    assertEquals("Jane", filterGenerated.getValue());
  }
}
