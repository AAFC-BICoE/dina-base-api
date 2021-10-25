package ca.gc.aafc.dina.repository;

import javax.persistence.criteria.Expression;
import javax.persistence.criteria.From;
import javax.persistence.criteria.JoinType;
import java.util.List;

/**
 * Provides methods for handling sparse field sets and inclusion of related resources.
 */
public final class SelectionHandler {

  private SelectionHandler() {
  }

  /**
   * Gets a JPA expression given a base path and an attributePath. Works as a short-hand method to get
   * expressions that could require joins. This method could be rewritten later to map DTO fields to custom
   * expressions.
   *
   * @param basePath      the base path
   * @param attributePath the attribute path
   * @return the expression
   */
  public static Expression<?> getExpression(From<?, ?> basePath, List<String> attributePath) {
    From<?, ?> from = basePath;
    for (String pathElement : attributePath.subList(0, attributePath.size() - 1)) {
      from = from.join(pathElement, JoinType.LEFT);
    }
    return from.get(attributePath.get(attributePath.size() - 1));
  }

}
