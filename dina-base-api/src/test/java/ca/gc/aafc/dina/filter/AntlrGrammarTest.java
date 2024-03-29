package ca.gc.aafc.dina.filter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class AntlrGrammarTest {

  @Test
  public void onFilterAsString_structureReturned() {
    String content =
      "filter[name][EQ]=2&filter[position][NEQ]=manager,supervisor&sort=position,-name&page[offset]=5&page[limit]=1&include=author.name";

    String content2 = "filter[createdBy.name][EQ]=John Doe&filter[createdBy.age][GT]=30&"+
      "filter[updatedBy.name][NEQ]=Jane&filter[position][LT]=5&sort=position,-name&"+
      "page[limit]=10&page[offset]=20&include=author._name,author.age";

    QueryComponent queryComponent = QueryStringParser.parse(content);

    assertNotNull(queryComponent.getFilters());

    assertEquals("position", queryComponent.getSorts().get(0));
    assertEquals("-name", queryComponent.getSorts().get(1));
    assertEquals("author.name", queryComponent.getIncludes().get(0));

    assertEquals(5, queryComponent.getPageOffset());
    assertEquals(1, queryComponent.getPageLimit());

    QueryComponent queryComponent2 = QueryStringParser.parse(content2);

    assertNotNull(queryComponent2.getFilters());

    FilterGroup fg1 = queryComponent2.getFilterGroup().orElseThrow();
    assertEquals("John Doe", ((FilterExpression)fg1.getComponents().get(0)).value());
    assertEquals("position", queryComponent2.getSorts().get(0));
    assertEquals("-name", queryComponent2.getSorts().get(1));
    assertEquals("author._name", queryComponent2.getIncludes().get(0));
    assertEquals("author.age", queryComponent2.getIncludes().get(1));

    assertEquals(20, queryComponent2.getPageOffset());
    assertEquals(10, queryComponent2.getPageLimit());
  }

  @Test
  public void onNoFilterAsString_structureReturned() {
    String content = "sort=position,-name&page[offset]=5&page[limit]=1&include=author.name";

    QueryComponent queryComponent = QueryStringParser.parse(content);

    assertEquals("position", queryComponent.getSorts().get(0));
    assertEquals("-name", queryComponent.getSorts().get(1));
    assertEquals("author.name", queryComponent.getIncludes().get(0));

    assertEquals(5, queryComponent.getPageOffset());
    assertEquals(1, queryComponent.getPageLimit());
  }

  
}
