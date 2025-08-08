package ca.gc.aafc.dina.filter;

import java.io.IOException;
import java.io.StringReader;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
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
      .fiql(listener.getFiql())
      .fields(listener.getFields())
      .includes(listener.getInclude() != null ? Set.copyOf(listener.getInclude()) : null)
      .sorts(listener.getSort())
      .pageLimit(listener.getPageLimit())
      .pageOffset(listener.getPageOffset())
      .build();
  }

  /**
   * Parses a comma-separated string of values, handling quoted strings with embedded commas.
   *
   * <p>Uses CSV format: quotes are optional but required for values containing commas or quotes.
   * Quotes within values are escaped with backslashes, whitespace is trimmed.</p>
   *
   * <p>Examples:
   * <ul>
   *   <li>{@code "apple,banana,cherry"} → {"apple", "banana", "cherry"}</li>
   *   <li>{@code "apple,\"red, apple\",banana"} → {"apple", "red, apple", "banana"}</li>
   *   <li>{@code "\"He said \\\"Hi\\\"\",world"} → {"He said \"Hi\"", "world"}</li>
   * </ul>
   *
   * @param values comma-separated string to parse, must not be null
   * @return set of parsed values, empty values excluded
   * @throws IllegalArgumentException if values is null or parsing fails
   */
  public static Set<String> parseQuotedValues(String values) {
    try {
      CSVFormat format = CSVFormat.DEFAULT
        .builder()
        .setQuote('"')
        .setEscape('\\')
        .setIgnoreSurroundingSpaces(true)
        .get();

      CSVParser parser = format.parse(new StringReader(values));
      CSVRecord record = parser.iterator().next();

      return StreamSupport.stream(record.spliterator(), false)
        .collect(Collectors.toSet());

    } catch (IOException e) {
      throw new IllegalArgumentException("Failed to parse values: " + values, e);
    }
  }
}
