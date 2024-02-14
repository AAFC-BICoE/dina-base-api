package ca.gc.aafc.dina.filter;

import java.util.ArrayList;
import java.util.List;

import com.querydsl.core.types.Ops;

import ca.gc.aafc.dina.filter.simple.SimpleSearchFilterBaseListener;
import ca.gc.aafc.dina.filter.simple.SimpleSearchFilterParser;

public class AntlBasedSimpleSearchFilterListener extends SimpleSearchFilterBaseListener {

  private final List<FilterComponent> components = new ArrayList<>();
  private final List<String> includes = new ArrayList<>();
  private final List<String> sortAttributes = new ArrayList<>();

  @Override
  public void exitFilter(SimpleSearchFilterParser.FilterContext ctx) {
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

  public void exitInclude(SimpleSearchFilterParser.IncludeContext ctx) {
    for(var attribute :  ctx.propertyName()) {
      includes.add(attribute.getText());
    }
  }

  @Override
  public void exitSort(SimpleSearchFilterParser.SortContext ctx) {
    for(var attribute :  ctx.sortPropertyName()) {
      sortAttributes.add(attribute.getText());
    }
  }

  /**
   * Build the FilterComponent object once the filter is parsed.
   * @return
   */
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

  public List<String> getInclude() {
    return includes;
  }

  public List<String> getSort() {
    return sortAttributes;
  }

  public Ops translateOperator(String op) {
    return switch (op) {
      case "EQ" -> Ops.EQ;
      case "NEQ" -> Ops.NE;
      default -> null;
    };
  }

}
