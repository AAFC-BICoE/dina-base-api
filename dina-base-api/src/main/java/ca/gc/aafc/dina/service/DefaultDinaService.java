package ca.gc.aafc.dina.service;

import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.jpa.BaseDAO;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * Service class for database interactions with a {@link DinaEntity}.
 *
 * @param <E> - Type of {@link DinaEntity}
 */
@Component
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class DefaultDinaService<E extends DinaEntity> implements DinaService<E> {

  @NonNull
  private final BaseDAO baseDAO;

  /**
   * Persist an instance of the provided entity in the database.
   *
   * @param entity entity to persist
   * @return returns the original entity.
   */
  @Override
  public E create(E entity) {
    preCreate(entity);
    validate(entity);
    baseDAO.create(entity);
    return entity;
  }

  /**
   * Merge the state of a given entity into the current persistence context.
   *
   * @param entity entity to update
   * @return returns the managed instance the state was merged to.
   */
  @Override
  public E update(E entity) {
    preUpdate(entity);
    validate(entity);
    return baseDAO.update(entity);
  }

  /**
   * Remove the given entity from the database.
   *
   * @param entity entity to delete
   */
  @Override
  public void delete(E entity) {
    preDelete(entity);
    baseDAO.delete(entity);
  }

  /**
   * Returns a list of Entities of a given class restricted by the predicates returned by a given
   * function.
   *
   * @param entityClass - entity class to query cannot be null
   * @param where       - function to return the predicates cannot be null
   * @param orderBy     - function to return the sorting criteria can be null
   * @param startIndex  - position of first result to retrieve
   * @param maxResult   - maximun number of results to return
   * @return list of entities
   */
  @Override
  public <T> List<T> findAll(
    @NonNull Class<T> entityClass,
    @NonNull BiFunction<CriteriaBuilder, Root<T>, Predicate[]> where,
    BiFunction<CriteriaBuilder, Root<T>, List<Order>> orderBy,
    int startIndex,
    int maxResult
  ) {
    CriteriaBuilder criteriaBuilder = baseDAO.getCriteriaBuilder();
    CriteriaQuery<T> criteria = criteriaBuilder.createQuery(entityClass);
    Root<T> root = criteria.from(entityClass);

    criteria.where(where.apply(criteriaBuilder, root)).select(root);
    if (orderBy != null) {
      criteria.orderBy(orderBy.apply(criteriaBuilder, root));
    }
    return baseDAO.resultListFromCriteria(criteria, startIndex, maxResult);
  }

  /**
   * Returns the resource count from a given predicate supplier.
   *
   * @param entityClass       - entity class to query cannot be null
   * @param predicateSupplier - function to return the predicates cannot be null
   * @return resource count
   */
  @Override
  public <T> Long getResourceCount(
    @NonNull Class<T> entityClass,
    @NonNull BiFunction<CriteriaBuilder, Root<T>, Predicate[]> predicateSupplier
  ) {
    return baseDAO.getResourceCount(entityClass, predicateSupplier);
  }

  /**
   * Find an entity by it's NaturalId. The method assumes that the naturalId is unique.
   *
   * @param naturalId   - id of entity
   * @param entityClass - class of entity
   * @return the matched entity
   */
  @Override
  public <T> T findOne(Object naturalId, Class<T> entityClass) {
    return baseDAO.findOneByNaturalId(naturalId, entityClass);
  }

  /**
   * Returns a reference to an entity that should exist without actually loading it. Useful to set
   * relationships without loading the entity instead of findOne.
   *
   * @param naturalId   - natural id of entity
   * @param entityClass - class of entity
   * @return the matched reference
   */
  @Override
  public <T> T getReferenceByNaturalId(Class<T> entityClass, Object naturalId) {
    return baseDAO.getReferenceByNaturalId(entityClass, naturalId);
  }

  /**
   * Check for the existence of a record by natural id.
   */
  @Override
  public boolean exists(Class<?> entityClass, Object naturalId) {
    return baseDAO.existsByNaturalId(naturalId, entityClass);
  }

  /**
   * Run before the {@link DefaultDinaService#create(DinaEntity)} method.
   *
   * @param entity entity being created by {@link DefaultDinaService#create(DinaEntity)}
   */
  protected void preCreate(E entity) {
    // Defaults to do nothing
  }

  /**
   * Run before the {@link DefaultDinaService#update(DinaEntity)} method.
   *
   * @param entity entity being updated by {@link DefaultDinaService#update(DinaEntity)}
   */
  protected void preUpdate(E entity) {
    // Defaults to do nothing
  }

  /**
   * Run before the {@link DefaultDinaService#delete(DinaEntity)} method.
   *
   * @param entity entity being deleted by {@link DefaultDinaService#delete(DinaEntity)}
   */
  protected void preDelete(E entity) {
    // Defaults to do nothing
  }

  private void validate(E entity) {
    Set<ConstraintViolation<E>> constraintViolations = baseDAO.validateEntity(entity);
    if (CollectionUtils.isNotEmpty(constraintViolations)) {
      throw new ConstraintViolationException(constraintViolations);
    }
  }

}
