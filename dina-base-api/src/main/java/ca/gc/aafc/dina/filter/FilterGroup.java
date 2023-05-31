package ca.gc.aafc.dina.filter;

import java.util.List;

import lombok.Getter;
import lombok.Singular;
import lombok.Builder;

@Getter
@Builder
public final class FilterGroup implements FilterComponent {
  public enum Conjunction {
    AND,
    OR
  }

  /**
   * Conjunction currently set for the components.
   */
  private final Conjunction conjunction;

  /**
   * All of the filter components for this group. Groups can also be nested here
   * so you can have
   * groups inside groups.
   */
  @Singular
  private final List<FilterComponent> components;

  private FilterGroup(Conjunction conjunction, List<FilterComponent> components) {
    this.conjunction = conjunction;
    this.components = components;
  }

  /**
   * Checks if the list of filter components contains one or more items.
   *
   * @return {@code true} if the list contains one or more items, {@code false}
   *         otherwise.
   */
  public boolean hasComponents() {
    return components != null && !components.isEmpty();
  }
}
