package ca.gc.aafc.dina.filter;

import java.util.Set;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.commons.lang3.StringUtils;

import ca.gc.aafc.dina.filter.simple.SimpleSearchFilterLexer;
import ca.gc.aafc.dina.filter.simple.SimpleSearchFilterParser;

/**
 * Responsible to parse a query string into a {@link QueryComponent}.
 */
public final class QueryStringParser {

  private QueryStringParser() {
    // utility class
  }

  /**
   * Parse the given query string in {@link QueryComponent}.
   *
   * @param queryString the query string or blank/null
   * @return the {@link QueryComponent} or {@link QueryComponent#EMPTY} is the query string is blank
   */
  public static QueryComponent parse(String queryString) {

    if (StringUtils.isBlank(queryString)) {
      return QueryComponent.EMPTY;
    }

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
      .includes(listener.getInclude() != null ? Set.copyOf(listener.getInclude()) : null)
      .sorts(listener.getSort())
      .pageLimit(listener.getPageLimit())
      .pageOffset(listener.getPageOffset())
      .build();
  }

}
