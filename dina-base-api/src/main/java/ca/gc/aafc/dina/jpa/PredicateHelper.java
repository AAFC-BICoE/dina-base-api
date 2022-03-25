package ca.gc.aafc.dina.jpa;

import org.apache.commons.lang3.tuple.Pair;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.List;

/**
 * Utility class to help manipulating {@link Predicate}
 */
public final class PredicateHelper {

  private PredicateHelper() {
  }

  /**
   * Add to the provided Predicate (if provided otherwise a new one will be created) clauses for each pair provided (AND separated).
   * @param currentPredicate current predicate or null to build a new one
   * @param criteriaBuilder
   * @param root
   * @param propertiesAndValue
   * @return
   */
  public static Predicate appendPropertiesEqual(Predicate currentPredicate,
      CriteriaBuilder criteriaBuilder, Root<?> root,
      List<Pair<String, Object>> propertiesAndValue) {

    Predicate updatedPredicate = currentPredicate;
    for (Pair<String, Object> propVal : propertiesAndValue) {
      Predicate clause = criteriaBuilder.equal(root.get(propVal.getKey()), propVal.getValue());
      updatedPredicate =
          updatedPredicate == null ? clause : criteriaBuilder.and(updatedPredicate, clause);
    }
    return updatedPredicate;
  }
}
