package ca.gc.aafc.dina.filter;

import java.util.ArrayList;
import java.util.List;

import ca.gc.aafc.dina.filter.simple.SimpleSearchFilterLexer;
import ca.gc.aafc.dina.filter.simple.SimpleSearchFilterBaseListener;
import ca.gc.aafc.dina.filter.simple.SimpleSearchFilterParser;

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
    String content2 = "sort=position,-name&page[offset]=5&page[limit]=1&include=author.name";

    SimpleSearchFilterLexer lexer = new SimpleSearchFilterLexer(
      CharStreams.fromString(content));
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    SimpleSearchFilterParser parser = new SimpleSearchFilterParser(tokens);

    // root
    ParseTree tree = parser.simpleFilter();

    ParseTreeWalker walker = new ParseTreeWalker();
    FilterComponentListener listener= new FilterComponentListener();
    walker.walk(listener, tree);


    FilterComponent fc = listener.buildFilterComponent();
    assertNotNull(fc);
  }

  public static class FilterComponentListener extends SimpleSearchFilterBaseListener {
    private final List<FilterComponent> components = new ArrayList<>();
    private final List<String> sortAttributes = new ArrayList<>();

    @Override
    public void exitSimpleFilter(SimpleSearchFilterParser.SimpleFilterContext ctx) {
      System.out.println("exit simple filter");
    }

    @Override
    public void exitExpression(SimpleSearchFilterParser.ExpressionContext ctx) {
      System.out.println("exit expression");
    }


    @Override
    public void exitFilter(SimpleSearchFilterParser.FilterContext ctx) {
      System.out.println("exit filter");
      // more than 1 value means a OR
      if (ctx.attributeValue().size() > 1) {
        FilterGroup.FilterGroupBuilder fgBuilder =
          FilterGroup.builder().conjunction(FilterGroup.Conjunction.OR);

        for (var filterValue : ctx.attributeValue()) {
          fgBuilder.component(new FilterExpression(ctx.propertyName().getText(),
            translateOperator(ctx.comparison().getText()), filterValue.getText()));
        }
        components.add(fgBuilder.build());
      } else if (ctx.attributeValue().size() == 1) {
        components.add(new FilterExpression(ctx.propertyName().getText(),
          translateOperator(ctx.comparison().getText()), ctx.attributeValue().get(0).getText()));
      }
    }

    @Override
    public void exitSort(SimpleSearchFilterParser.SortContext ctx) {
      System.out.println("exit sort");
      for(var attribute :  ctx.sortPropertyName()) {
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
