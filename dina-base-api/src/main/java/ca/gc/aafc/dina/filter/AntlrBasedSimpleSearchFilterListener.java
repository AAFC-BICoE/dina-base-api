package ca.gc.aafc.dina.filter;

import java.util.ArrayList;
import java.util.List;

import com.querydsl.core.types.Ops;

import ca.gc.aafc.dina.filter.simple.SimpleSearchFilterBaseListener;
import ca.gc.aafc.dina.filter.simple.SimpleSearchFilterParser;

/**
 * Antlr-based listener to build specific components.
 *
 * Package-protected, implementation details. {@link QueryStringParser} should be used.
 */
class AntlrBasedSimpleSearchFilterListener extends SimpleSearchFilterBaseListener {

  private static final String DEFAULT_OP = "EQ";

  private final List<FilterComponent> components = new ArrayList<>();
  private final List<String> includes = new ArrayList<>();
  private final List<String> sortAttributes = new ArrayList<>();

  private Integer pageOffset;
  private Integer pageLimit;

  @Override
  public void exitFilter(SimpleSearchFilterParser.FilterContext ctx) {
    // more than 1 value means a OR
    if (ctx.attributeValue().size() > 1) {
      FilterGroup.FilterGroupBuilder fgBuilder =
        FilterGroup.builder().conjunction(FilterGroup.Conjunction.OR);

      for (var filterValue : ctx.attributeValue()) {
        fgBuilder.component(new FilterExpression(ctx.propertyName().getText(),
          translateOperator(extractComparison(ctx)), filterValue.getText()));
      }
      components.add(fgBuilder.build());
    } else if (ctx.attributeValue().size() == 1) {
      components.add(new FilterExpression(ctx.propertyName().getText(),
        translateOperator(extractComparison(ctx)), ctx.attributeValue().getFirst().getText()));
    }
  }

  @Override
  public void exitInclude(SimpleSearchFilterParser.IncludeContext ctx) {
    for (var attribute :  ctx.propertyName()) {
      includes.add(attribute.getText());
    }
  }

  @Override
  public void exitSort(SimpleSearchFilterParser.SortContext ctx) {
    for (var attribute :  ctx.sortPropertyName()) {
      sortAttributes.add(attribute.getText());
    }
  }

  @Override
  public void exitPage(SimpleSearchFilterParser.PageContext ctx) {
    if (ctx.getText().contains("offset")) {
      pageOffset = Integer.valueOf(ctx.pageValue().getText());
    } else if (ctx.getText().contains("limit")) {
      pageLimit = Integer.valueOf(ctx.pageValue().getText());
    }
  }

  /**
   * Comparison operator is optional, this method will return the default operator
   * if absent.
   * @param ctx
   * @return
   */
  private static String extractComparison(SimpleSearchFilterParser.FilterContext ctx) {
    if (ctx.comparison() == null) {
      return DEFAULT_OP;
    }

    return ctx.comparison().getText();
  }

  /**
   * Build the FilterComponent object once the filter is parsed.
   * @return
   */
  public FilterComponent buildFilterComponent() {
    if (components.size() == 1) {
      return components.get(0);
    } else if (components.size() > 1) {
      return
        FilterGroup.builder().conjunction(FilterGroup.Conjunction.AND)
          .components(components).build();
    }
    return null;
  }

  public List<String> getInclude() {
    return includes;
  }

  public List<String> getSort() {
    return sortAttributes;
  }

  public Integer getPageOffset() {
    return pageOffset;
  }

  public Integer getPageLimit() {
    return pageLimit;
  }

  public Ops translateOperator(String op) {
    return switch (op) {
      case "EQ" -> Ops.EQ;
      case "NEQ" -> Ops.NE;
      case "LT" -> Ops.LT;
      case "GT" -> Ops.GT;
      default -> null;
    };
  }

}
