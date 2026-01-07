package ca.gc.aafc.dina.filter;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.SingularAttribute;

import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.ext.search.PropertyNotFoundException;
import org.apache.cxf.jaxrs.ext.search.SearchCondition;
import org.apache.cxf.jaxrs.ext.search.SearchParseException;
import org.apache.cxf.jaxrs.ext.search.fiql.FiqlParser;
import org.apache.cxf.jaxrs.ext.search.jpa.JPACriteriaQueryVisitor;

import ca.gc.aafc.dina.exception.UnknownAttributeException;

/**
 * FIQL query handling
 * <a href="https://datatracker.ietf.org/doc/html/draft-nottingham-atompub-fiql-00">...</a>
 */
public final class FIQLFilterHandler {

  private FIQLFilterHandler() {
    //utility class
  }

  public static <T, E> CriteriaQuery<E> criteriaQuery(EntityManager em, String fiqlQuery,
                                                      Class<T> clazz, Class<E> clazz2,
                                                      List<String> orderBy) {
    // Create parser for the entity type
    FiqlParser<T> parser = new FiqlParser<>(clazz);

    // Parse FIQL string into SearchCondition
    try {
      SearchCondition<T> condition = parser.parse(fiqlQuery);
      // Convert to JPA Predicate using a visitor JPACriteriaQueryVisitor
      JPACriteriaQueryVisitor<T, E> visitor = new JPACriteriaQueryVisitor<>(em, clazz, clazz2);
      condition.accept(visitor);

      if (orderBy != null && !orderBy.isEmpty()) {
        EntityType<T> entityType = em.getMetamodel().entity(clazz);
        List<SingularAttribute<T, ?>> orderByClause = new ArrayList<>();
        // JPACriteriaQueryVisitor only supports 1 asc/desc so we only honor the first one
        boolean asc = true;
        for (String oderByElement : orderBy) {
          asc = !oderByElement.startsWith(EntityFilterHelper.REVERSE_ORDER_PREFIX);
          SingularAttribute<T, String> orderByAttribute =
            (SingularAttribute<T, String>) entityType.getSingularAttribute(
              StringUtils.removeStart(oderByElement, EntityFilterHelper.REVERSE_ORDER_PREFIX));
          orderByClause.add(orderByAttribute);
        }
        visitor.orderBy(orderByClause, asc);
      }

      return visitor.getQuery();
    } catch (PropertyNotFoundException ex) {
      // getMessage of PropertyNotFoundException is null so we need to construct one
      throw new UnknownAttributeException(ex.getName());
    } catch (SearchParseException ex) {
      throw new IllegalArgumentException(ex);
    }
  }

  public static <T> Long count(EntityManager em, String fiqlQuery,
                                                      Class<T> clazz) {

    // Create parser for the entity type
    FiqlParser<T> parser = new FiqlParser<>(clazz);
    // Parse FIQL string into SearchCondition
    try {
      SearchCondition<T> condition = parser.parse(fiqlQuery);
      // Convert to JPA Predicate using a visitor JPACriteriaQueryVisitor
      JPACriteriaQueryVisitor<T, Long> visitor = new JPACriteriaQueryVisitor<>(em, clazz, Long.class);
      condition.accept(visitor);

      return visitor.count();
    } catch (SearchParseException ex) {
      throw new IllegalArgumentException(ex);
    }
  }

}
