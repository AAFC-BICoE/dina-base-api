package ca.gc.aafc.dina.filter;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Builder;

@Getter
@Builder
public class FilterGroup implements FilterComponent {
  public enum Conjunction {
    AND,
    OR
  }

  /**
   * Conjunction currently set for the expressions.
   */
  private final Conjunction conjunction;

  /**
   * All of the filter expressions for this group. Groups can also be nested here so you can have
   * groups inside groups.
   */
  private final List<FilterComponent> expressions;

  private FilterGroup(Conjunction conjunction, List<FilterComponent> expressions) {
    this.conjunction = conjunction;
    this.expressions = expressions;
  }

  /**
   * Checks if the list of filter expressions contains one or more items.
   *
   * @return {@code true} if the list contains one or more items, {@code false} otherwise.
   */
  public boolean hasExpressions() {
    return expressions != null && !expressions.isEmpty();
  }

  public static class FilterGroupBuilder {
    private List<FilterComponent> expressions = new ArrayList<>();

    public FilterGroupBuilder add(FilterComponent component) {
        expressions.add(component);
        return this;
    }

    public FilterGroupBuilder addAll(List<FilterComponent> components) {
        expressions.addAll(components);
        return this;
    }
  }
}
