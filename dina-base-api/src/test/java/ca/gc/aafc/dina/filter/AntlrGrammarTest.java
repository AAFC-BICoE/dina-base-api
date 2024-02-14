package ca.gc.aafc.dina.filter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AntlrGrammarTest {

  @Test
  public void onFilterAsString_structureReturned() {
    String content =
      "filter[name][EQ]=2&filter[position][NEQ]=manager,supervisor&sort=position,-name&page[offset]=5&page[limit]=1&include=author.name";
    String content2 = "sort=position,-name&page[offset]=5&page[limit]=1&include=author.name";

    QueryComponent queryComponent = QueryStringParser.parse(content);


    assertEquals("position", queryComponent.getSorts().get(0));
    assertEquals("-name", queryComponent.getSorts().get(1));
    assertEquals("author.name", queryComponent.getIncludes().get(0));

    assertEquals(5, queryComponent.getPageOffset());
    assertEquals(1, queryComponent.getPageLimit());
  }

}
