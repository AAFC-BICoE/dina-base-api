package ca.gc.aafc.dina.filter;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.junit.jupiter.api.Test;

import ca.gc.aafc.dina.filter.simple.SimpleSearchFilterLexer;
import ca.gc.aafc.dina.filter.simple.SimpleSearchFilterParser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class AntlrGrammarTest {

  @Test
  public void onFilterAsString_structureReturned() {
    String content = "filter[name][EQ]=2&filter[position][NEQ]=manager,supervisor&sort=position,-name&page[offset]=5&page[limit]=1&include=author.name";
    String content2 = "sort=position,-name&page[offset]=5&page[limit]=1&include=author.name";

    SimpleSearchFilterLexer lexer = new SimpleSearchFilterLexer(
      CharStreams.fromString(content));
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    SimpleSearchFilterParser parser = new SimpleSearchFilterParser(tokens);

    // root
    ParseTree tree = parser.simpleFilter();

    ParseTreeWalker walker = new ParseTreeWalker();
    AntlrBasedSimpleSearchFilterListener listener= new AntlrBasedSimpleSearchFilterListener();
    walker.walk(listener, tree);


    FilterComponent fc = listener.buildFilterComponent();
    assertNotNull(fc);

    assertEquals("position", listener.getSort().get(0));
    assertEquals("-name", listener.getSort().get(1));
    assertEquals("author.name", listener.getInclude().get(0));

    assertEquals(5, listener.getPageOffset());
    assertEquals(1, listener.getPageLimit());
  }

}
