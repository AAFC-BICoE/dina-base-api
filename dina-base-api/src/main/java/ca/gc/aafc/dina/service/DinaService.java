package ca.gc.aafc.dina.service;

import ca.gc.aafc.dina.entity.DinaEntity;
import lombok.NonNull;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.List;
import java.util.function.BiFunction;

public interface DinaService<E extends DinaEntity> {

  E create(E entity);

  E update(E entity);

  void delete(E entity);

  <T> T findOne(Object naturalId, Class<T> entityClass);

  <T> T findOneReferenceByNaturalId(Class<T> entityClass, Object naturalId);

  <T> List<T> findAll(
    @NonNull Class<T> entityClass,
    @NonNull BiFunction<CriteriaBuilder, Root<T>, Predicate[]> where,
    BiFunction<CriteriaBuilder, Root<T>, List<Order>> orderBy,
    int startIndex,
    int maxResult
  );

  <T> Long getResourceCount(
    @NonNull Class<T> entityClass,
    @NonNull BiFunction<CriteriaBuilder, Root<T>, Predicate[]> predicateSupplier
  );

  boolean exists(Class<?> entityClass, Object naturalId);

}
