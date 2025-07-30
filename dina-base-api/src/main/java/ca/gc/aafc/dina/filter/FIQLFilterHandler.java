package ca.gc.aafc.dina.filter;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaQuery;

import org.apache.cxf.jaxrs.ext.search.SearchCondition;
import org.apache.cxf.jaxrs.ext.search.fiql.FiqlParser;
import org.apache.cxf.jaxrs.ext.search.jpa.JPACriteriaQueryVisitor;

/**
 * FIQL query handling
 * <a href="https://datatracker.ietf.org/doc/html/draft-nottingham-atompub-fiql-00">...</a>
 */
public class FIQLFilterHandler {


  public static <T, E> CriteriaQuery<E> criteriaQuery(EntityManager em, String fiqlQuery,
                                                     Class<T> clazz, Class<E> clazz2) {
    // Create parser for your entity type
    FiqlParser<T> parser = new FiqlParser<>(clazz);

    // Parse FIQL string into SearchCondition
    // String fiqlQuery = "name==John;age=gt=18";
    SearchCondition<T> condition = parser.parse(fiqlQuery);

    // Convert to JPA Predicate using a visitor JPACriteriaQueryVisitor
    JPACriteriaQueryVisitor<T, E> visitor = new JPACriteriaQueryVisitor<>(em, clazz, clazz2);
    condition.accept(visitor);

    return visitor.getQuery();
  }
}
