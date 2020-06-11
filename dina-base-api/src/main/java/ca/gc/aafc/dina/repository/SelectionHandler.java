package ca.gc.aafc.dina.repository;

import java.util.List;

import javax.persistence.criteria.Expression;
import javax.persistence.criteria.From;
import javax.persistence.criteria.JoinType;

import io.crnk.core.engine.registry.ResourceRegistry;

/**
 * Provides methods for handling sparse field sets and inclusion of related resources.
 */
public final class SelectionHandler {

  private SelectionHandler() { }
  
  /**
   * Gets a JPA expression given a base path and an attributePath. Works as a short-hand method to
   * get expressions that could require joins.
   * This method could be rewritten later to map DTO fields to custom expressions.
   * 
   * @param basePath the base path
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
  
  /**
   * Gets the name of the JPA entity's @Id attribute
   * 
   * @return
   */
  public static String getIdAttribute(Class<?> resourceClass, ResourceRegistry resourceRegistry) {
    return resourceRegistry.findEntry(resourceClass)
        .getResourceInformation()
        .getIdField()
        .getUnderlyingName();
  }
  
  /**
   * Gets the resource class' @JsonApiId attribute.
   * 
   * @return the JPA Expression of the Id attribute.
   */
  public static Expression<?> getIdExpression(
      From<?, ?> root,
      Class<?> resourceClass,
      ResourceRegistry resourceRegistry
  ) {
    return root.get(getIdAttribute(resourceClass, resourceRegistry));
  }
  
}
