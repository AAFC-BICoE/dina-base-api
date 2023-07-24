package ca.gc.aafc.dina.filter;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.junit.jupiter.api.Test;

public class AntlrGrammarTest {

  @Test
  public void a() {
    String content = "filter[name][EQ]=jim&filter[position][NEQ]=manager";
    ca.gc.aafc.dina.filter.SimpleSearchFilterLexer lexer = new ca.gc.aafc.dina.filter.SimpleSearchFilterLexer(
      CharStreams.fromString(content));
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    ca.gc.aafc.dina.filter.SimpleSearchFilterParser parser = new ca.gc.aafc.dina.filter.SimpleSearchFilterParser(tokens);


    ParseTree tree = parser.filterExpression();

    ParseTreeWalker walker = new ParseTreeWalker();
    ca.gc.aafc.dina.filter.SimpleSearchFilterBaseListener listener= new MyListener();
    walker.walk(listener, tree);
  }

  public static class MyListener extends ca.gc.aafc.dina.filter.SimpleSearchFilterBaseListener {

    @Override
    public void exitFilterExpression(ca.gc.aafc.dina.filter.SimpleSearchFilterParser.FilterExpressionContext ctx) {
      System.out.println("attribute:" + ctx.filterAttribute().getText());
      System.out.println("operator:" + ctx.filterOp().getText());
      System.out.println("values:" + ctx.filterValue().get(0).getText());
    }

  }
}
