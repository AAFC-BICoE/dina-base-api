package ca.gc.aafc.dina.filter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.antlr.v4.runtime.tree.ParseTreeProperty;

import lombok.Getter;

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
  private final Map<String, List<String>> fields = new HashMap<>();
  private final Map<String, List<String>> optFields = new HashMap<>();
  private final List<String> sortAttributes = new ArrayList<>();

  /**
   * Maps parser contexts to the FilterComponent built for that node.
   * Used to assemble nested AND/OR filter groups while traversing
   * the parse tree.
   */
  private final ParseTreeProperty<FilterComponent> built = new ParseTreeProperty<>();

  @Getter
  private String fiql;

  @Getter
  private Integer pageOffset;

  @Getter
  private Integer pageLimit;

  @Override
  public void exitFilter(SimpleSearchFilterParser.FilterContext ctx) {

    FilterComponent component;
    // more than 1 value means a OR
    if (ctx.attributeValue().size() > 1) {
      FilterGroup.FilterGroupBuilder fgBuilder =
        FilterGroup.builder().conjunction(FilterGroup.Conjunction.OR);

      for (var filterValue : ctx.attributeValue()) {
        fgBuilder.component(new FilterExpression(ctx.propertyName().getText(),
          translateOperator(extractComparison(ctx)), filterValue.getText()));
      }
      component = fgBuilder.build();
    } else {
      component = new FilterExpression(ctx.propertyName().getText(),
        translateOperator(extractComparison(ctx)), ctx.attributeValue().getFirst().getText());
    }
    built.put(ctx, component);
  }

  /**
   * Resolves a primary filter expression to the FilterComponent previously
   * built for either a filter predicate or a nested parenthesized expression.
   */
  @Override
  public void exitFilterPrimary( SimpleSearchFilterParser.FilterPrimaryContext ctx) {

    if (ctx.filter() != null) {
      built.put(ctx, built.get(ctx.filter()));
    } else {
      built.put(ctx, built.get(ctx.filterOrExpression()));
    }
  }

  /**
   * Builds a FilterGroup using the AND conjunction.
   *
   * All child filter expressions or nested filter groups have already been
   * constructed and stored in {@code built}. This method combines them into
   * a single FilterGroup when more than one operand is present.
   *
   * Examples:
   * (filter[a][EQ]=1&filter[b][EQ]=2)
   * ((filter[a][EQ]=1|filter[b][EQ]=2)&filter[c][EQ]=3)
   */
  @Override
  public void exitFilterAndExpression(SimpleSearchFilterParser.FilterAndExpressionContext ctx) {

    if (ctx.filterPrimary().size() == 1) {
      built.put(
          ctx,
          built.get(ctx.filterPrimary(0)));
      return;
    }

    FilterGroup.FilterGroupBuilder builder = FilterGroup.builder()
        .conjunction(FilterGroup.Conjunction.AND);

    for (var primary : ctx.filterPrimary()) {
      builder.component(built.get(primary));
    }

    built.put(ctx, builder.build());
  }

  /**
   * Builds a FilterGroup using the OR conjunction.
   *
   * All child filter expressions or nested filter groups have already been
   * constructed and stored in {@code built}. This method combines them into
   * a single FilterGroup when more than one operand is present.
   *
   * Examples:
   * (filter[a][EQ]=1|filter[b][EQ]=2)
   * (filter[firstName][EQ]=John|filter[lastName][EQ]=John)
   */
  @Override
  public void exitFilterOrExpression(
      SimpleSearchFilterParser.FilterOrExpressionContext ctx) {

    if (ctx.filterAndExpression().size() == 1) {
      built.put(ctx, built.get(ctx.filterAndExpression(0)));
      return;
    }

    FilterGroup.FilterGroupBuilder builder = FilterGroup.builder()
        .conjunction(FilterGroup.Conjunction.OR);

    for (var child : ctx.filterAndExpression()) {
      builder.component(built.get(child));
    }

    built.put(ctx, builder.build());
  }

  /**
   * Adds resolved filter components to the top-level component list.
   *
   * Individual filters and parenthesized filter groups are treated as
   * top-level filter components. Multiple top-level components remain
   * implicitly ANDed together when buildFilterComponent() is invoked.
   *
   * Examples:
   * filter[a][EQ]=1&filter[b][EQ]=2
   *
   * becomes:
   * AND(a, b)
   *
   * and:
   * (filter[a][EQ]=1|filter[b][EQ]=2)&filter[c][EQ]=3
   *
   * becomes:
   * AND(
   * OR(a, b),
   * c
   * )
   */
  @Override
  public void exitExpression(SimpleSearchFilterParser.ExpressionContext ctx) {

    if (ctx.filter() != null) {
      components.add(
          built.get(ctx.filter()));
    }

    if (ctx.filterGroup() != null) {
      components.add(
          built.get(ctx.filterGroup()));
    }
  }

  @Override
  public void exitFilterGroup(
      SimpleSearchFilterParser.FilterGroupContext ctx) {

    built.put(
        ctx,
        built.get(ctx.filterOrExpression()));
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
    try {
      if (ctx.getText().contains("offset")) {
        pageOffset = Integer.valueOf(ctx.pageValue().getText());
      } else if (ctx.getText().contains("limit")) {
        pageLimit = Integer.valueOf(ctx.pageValue().getText());
      }
    } catch (NumberFormatException ex) {
      throw new IllegalArgumentException(ex);
    }
  }

  @Override
  public void exitFields(SimpleSearchFilterParser.FieldsContext ctx) {
    List<String> fieldsForType = fields.computeIfAbsent(ctx.type().getText(),
      k -> new ArrayList<>());
    for (var property : ctx.propertyName()) {
      fieldsForType.add(property.getText());
    }
  }

  @Override
  public void exitOptFields(SimpleSearchFilterParser.OptFieldsContext ctx) {
    List<String> fieldsForType = optFields.computeIfAbsent(ctx.type().getText(),
      k -> new ArrayList<>());
    for (var property : ctx.propertyName()) {
      fieldsForType.add(property.getText());
    }
  }

  @Override
  public void exitFiql(SimpleSearchFilterParser.FiqlContext ctx) {
    fiql = ctx.fiqlPart().getText();
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
      return components.getFirst();
    } else if (components.size() > 1) {
      return
        FilterGroup.builder().conjunction(FilterGroup.Conjunction.AND)
          .components(components).build();
    }
    return null;
  }

  public Map<String, List<String>> getFields() {
    return fields;
  }

  public Map<String, List<String>> getOptFields() {
    return optFields;
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
      case "LT" -> Ops.LT;
      case "LOE" -> Ops.LOE;
      case "GT" -> Ops.GT;
      case "GOE" -> Ops.GOE;
      case "LIKE" -> Ops.LIKE;
      case "ILIKE" -> Ops.LIKE_IC;
      case "IN" -> Ops.IN;
      default -> null;
    };
  }
}
