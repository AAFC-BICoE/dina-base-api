package ca.gc.aafc.dina.filter;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Predicate;

import io.crnk.core.queryspec.QuerySpec;

/**
 * Interface for creating custom filtering strategies for the
 * JpaResourceRepository.
 */
public interface FilterHandler {
  Predicate getRestriction(QuerySpec querySpec, From<?, ?> root, CriteriaBuilder cb);
}
