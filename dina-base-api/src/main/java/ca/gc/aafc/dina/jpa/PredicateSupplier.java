package ca.gc.aafc.dina.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@FunctionalInterface
public interface PredicateSupplier<T> {
  Predicate[] supply(CriteriaBuilder criteriaBuilder, Root<T> root, EntityManager em);
}
