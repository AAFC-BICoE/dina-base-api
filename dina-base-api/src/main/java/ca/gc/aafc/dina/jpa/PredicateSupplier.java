package ca.gc.aafc.dina.jpa;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

@FunctionalInterface
public
interface PredicateSupplier<T> {
  Predicate[] supply(CriteriaBuilder criteriaBuilder, Root<T> root, EntityManager em);
}
