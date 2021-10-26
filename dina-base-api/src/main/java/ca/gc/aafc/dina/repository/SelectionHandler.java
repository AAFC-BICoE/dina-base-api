package ca.gc.aafc.dina.repository;

import org.apache.commons.collections.CollectionUtils;

import javax.persistence.criteria.From;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.Metamodel;
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
  public static Path<?> getExpression(Root<?> basePath, List<String> attributePath, Metamodel metamodel) {
    if (CollectionUtils.isEmpty(attributePath)) {
      return basePath;
    }
    From<?, ?> from = basePath;
    for (String pathElement : attributePath.subList(0, attributePath.size() - 1)) {
      ManagedType<?> managedType = metamodel.managedType(from.getJavaType());
      Attribute<?, ?> attribute = managedType.getAttribute(pathElement);
      Attribute.PersistentAttributeType persistentAttributeType = attribute.getPersistentAttributeType();
      if (Attribute.PersistentAttributeType.BASIC.equals(persistentAttributeType)) {
        return from.get(pathElement); // Exit at basic element no more joins possible
      } else {
        from = from.join(pathElement, JoinType.LEFT);
      }
    }
    return from.get(attributePath.get(attributePath.size() - 1));
  }

}
