package ca.gc.aafc.dina.filter;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import ca.gc.aafc.dina.filter.simple.SimpleSearchFilterLexer;
import ca.gc.aafc.dina.filter.simple.SimpleSearchFilterParser;

/**
 * Responsible to parse a query string into a {@link QueryComponent}.
 */
public class QueryStringParser {

  private QueryStringParser() {
    // utility class
  }

  public static QueryComponent parse(String queryString) {
    SimpleSearchFilterLexer lexer = new SimpleSearchFilterLexer(
      CharStreams.fromString(queryString));
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    SimpleSearchFilterParser parser = new SimpleSearchFilterParser(tokens);

    // root
    ParseTree tree = parser.simpleFilter();

    ParseTreeWalker walker = new ParseTreeWalker();
    AntlrBasedSimpleSearchFilterListener listener = new AntlrBasedSimpleSearchFilterListener();
    walker.walk(listener, tree);

    return QueryComponent.builder()
      .filters(listener.buildFilterComponent())
      .includes(listener.getInclude())
      .sorts(listener.getSort())
      .pageLimit(listener.getPageLimit())
      .pageOffset(listener.getPageOffset())
      .build();
  }

}
