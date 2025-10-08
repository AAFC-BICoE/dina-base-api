package ca.gc.aafc.dina.filter;

import org.junit.jupiter.api.Test;

import java.util.function.Predicate;

import com.querydsl.core.types.Ops;

import ca.gc.aafc.dina.filter.FilterComponentMutator.FilterComponentMutation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class FilterComponentMutatorsTest {

  @Test
  public void testFilterComponentMutator(){
    // Define the predicate (condition)
    Predicate<FilterComponent> predicate = component -> component instanceof FilterExpression expr && expr.attribute().equals("specificAttribute");

    FilterComponentMutation mutation = component -> {
      if (component instanceof FilterExpression expr) {
        return FilterGroup.builder()
          .conjunction(FilterGroup.Conjunction.AND)
          .component(new FilterExpression(expr.attribute(), Ops.EQ, expr.value()))
          .component(new FilterExpression("specificAttributeLength", Ops.EQ, ""+expr.value().length()))
          .build();
      }
      return component;
    };

    QueryComponent originalQuery = QueryComponent.builder()
      .filters(new FilterExpression("specificAttribute", Ops.EQ, "1900"))
      .build();

    // Apply the mutation
    QueryComponent transformed = FilterComponentMutator.mutate(
      originalQuery,
      predicate,
      mutation
    );

    assertInstanceOf(FilterGroup.class, transformed.getFilters());
    assertEquals(2, ((FilterGroup)transformed.getFilters()).getComponents().size());
  }
}
