package ca.gc.aafc.dina.filter;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.querydsl.core.types.Ops;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AntlrGrammarTest {

  @Test
  public void onFilterAsString_structureReturned2() {
    String content =
      "filter[type][EQ]=metadata&filter[name][EQ]=drawing.png&filter[group][EQ]=test";

    QueryComponent queryComponent = QueryStringParser.parse(content);
    queryComponent.getFilterExpression();
  }

  @Test
  public void onSparseFieldSet_structureReturned() {
    String content =
      "filter[type][EQ]=metadata&filter[name][EQ]=drawing.png&fields[metadata]=name,type";

    QueryComponent queryComponent = QueryStringParser.parse(content);
    List<String> fields = queryComponent.getFields().get("metadata");
    assertTrue(fields.contains("name"));
    assertTrue(fields.contains("type"));
  }

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
    assertTrue(queryComponent.getIncludes().contains("author.name"));

    assertEquals(5, queryComponent.getPageOffset());
    assertEquals(1, queryComponent.getPageLimit());

    QueryComponent queryComponent2 = QueryStringParser.parse(content2);

    assertNotNull(queryComponent2.getFilters());

    FilterGroup fg1 = queryComponent2.getFilterGroup().orElseThrow();
    assertEquals("John Doe", ((FilterExpression)fg1.getComponents().getFirst()).value());
    assertEquals("position", queryComponent2.getSorts().get(0));
    assertEquals("-name", queryComponent2.getSorts().get(1));
    assertTrue(queryComponent2.getIncludes().contains("author._name"));
    assertTrue(queryComponent2.getIncludes().contains("author.age"));

    assertEquals(20, queryComponent2.getPageOffset());
    assertEquals(10, queryComponent2.getPageLimit());
  }

  @Test
  public void onNoFilterAsString_structureReturned() {
    String content = "sort=position,-name&page[offset]=5&page[limit]=1&include=author.name";

    QueryComponent queryComponent = QueryStringParser.parse(content);

    assertEquals("position", queryComponent.getSorts().get(0));
    assertEquals("-name", queryComponent.getSorts().get(1));
    assertTrue(queryComponent.getIncludes().contains("author.name"));

    assertEquals(5, queryComponent.getPageOffset());
    assertEquals(1, queryComponent.getPageLimit());
  }

  @Test
  public void onEqWithList_structureReturned() {
    String content = "filter[name][EQ]=e1,e2";

    QueryComponent queryComponent = QueryStringParser.parse(content);
    FilterGroup fg = queryComponent.getFilterGroup().orElseThrow();

    assertEquals(2, fg.getComponents().size());

    // Name expected for both components.
    assertEquals("name", ((FilterExpression)fg.getComponents().getFirst()).attribute());
    assertEquals("name", ((FilterExpression)fg.getComponents().getLast()).attribute());

    // EQ operator expected for both components.
    assertEquals(Ops.EQ, ((FilterExpression)fg.getComponents().getFirst()).operator());
    assertEquals(Ops.EQ, ((FilterExpression)fg.getComponents().getLast()).operator());

    // Values expected for both components.
    assertEquals("e1", ((FilterExpression)fg.getComponents().getFirst()).value());
    assertEquals("e2", ((FilterExpression)fg.getComponents().getLast()).value());
    
    // Conjunction expected to be OR.
    assertEquals(FilterGroup.Conjunction.OR, fg.getConjunction());
  }

  @Test
  public void onNoOperator_EqualOperatorUsed() {
    String content = "filter[type]=metadata&filter[name]=drawing.png&filter[group][EQ]=test";

    QueryComponent queryComponent = QueryStringParser.parse(content);
    FilterGroup fg = queryComponent.getFilterGroup().orElseThrow();

    assertEquals("metadata", ((FilterExpression)fg.getComponents().getFirst()).value());
    assertEquals(Ops.EQ, ((FilterExpression)fg.getComponents().getFirst()).operator());
  }

  @Test
  public void onFiql_fiqlStringReturned() {
    String fiql = "updated=lt=2005-01-01T00:00:00Z,updated=lt=2005-01-03T00:00:00Z";
    String sort = "sort=title";
    String queryStr = "fiql=" + fiql + "&"+sort;

    QueryComponent queryComponent = QueryStringParser.parse(queryStr);
    assertEquals(fiql, queryComponent.getFiql());
    assertEquals("title", queryComponent.getSorts().getFirst());

    fiql = "name==jim*";
    queryStr = "fiql=" + fiql;
    queryComponent = QueryStringParser.parse(queryStr);
    assertEquals(fiql, queryComponent.getFiql());
  }

}
