package ca.gc.aafc.dina.filter;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.junit.jupiter.api.Test;

import com.querydsl.core.types.Ops;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class AntlrGrammarTest {

  @Test
  public void onFilterAsString_structureReturned() {
    String content = "filter[name][EQ]=2&filter[position][NEQ]=manager,supervisor&sort=position,-name&page[offset]=5&page[limit]=1&include=author.name";
    ca.gc.aafc.dina.filter.SimpleSearchFilterLexer lexer = new ca.gc.aafc.dina.filter.SimpleSearchFilterLexer(
      CharStreams.fromString(content));
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    ca.gc.aafc.dina.filter.SimpleSearchFilterParser parser = new ca.gc.aafc.dina.filter.SimpleSearchFilterParser(tokens);


    ParseTree tree = parser.filterExpression();

    ParseTreeWalker walker = new ParseTreeWalker();
    FilterComponentListener listener= new FilterComponentListener();
    walker.walk(listener, tree);

    FilterComponent fc = listener.buildFilterComponent();
    assertNotNull(fc);
  }

  public static class FilterComponentListener extends ca.gc.aafc.dina.filter.SimpleSearchFilterBaseListener {
    private final List<FilterComponent> components = new ArrayList<>();
    private final List<String> sortAttributes = new ArrayList<>();

    @Override
    public void exitFilterExpression(ca.gc.aafc.dina.filter.SimpleSearchFilterParser.FilterExpressionContext ctx) {
      // more than 1 value means a OR
      if (ctx.filterValue().size() > 1) {
        FilterGroup.FilterGroupBuilder fgBuilder =
          FilterGroup.builder().conjunction(FilterGroup.Conjunction.OR);

        for (var filterValue : ctx.filterValue()) {
          fgBuilder.component(new FilterExpression(ctx.attributeValue().getText(),
            translateOperator(ctx.filterOp().getText()), filterValue.getText()));
        }
        components.add(fgBuilder.build());
      } else if (ctx.filterValue().size() == 1) {
        components.add(new FilterExpression(ctx.attributeValue().getText(),
          translateOperator(ctx.filterOp().getText()), ctx.filterValue().get(0).getText()));
      }
    }

    @Override
    public void exitSortExpression(ca.gc.aafc.dina.filter.SimpleSearchFilterParser.SortExpressionContext ctx) {
      for(var attribute :  ctx.attributeValue()) {
        sortAttributes.add(attribute.getText());
      }
    }

    public FilterComponent buildFilterComponent() {
      if(components.size() == 1) {
        return components.get(0);
      } else if (components.size() > 1) {
        return
          FilterGroup.builder().conjunction(FilterGroup.Conjunction.AND)
            .components(components).build();
      }
      return null;
    }

    public Ops translateOperator(String op) {
      return switch (op) {
        case "EQ" -> Ops.EQ;
        case "NEQ" -> Ops.NE;
        default -> null;
      };
    }

  }
}
