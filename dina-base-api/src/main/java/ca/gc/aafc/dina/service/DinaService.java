package ca.gc.aafc.dina.service;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import javax.inject.Inject;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.stereotype.Component;

import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.jpa.BaseDAO;
import io.crnk.data.jpa.query.criteria.JpaCriteriaQueryFactory;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Service class for database interactions with a {@link DinaEntity}.
 *
 * @param <E> - Type of {@link DinaEntity}
 */
@Component
@RequiredArgsConstructor(onConstructor_ = @Inject)
public abstract class DinaService<E extends DinaEntity> {

  @NonNull
  private final BaseDAO baseDAO;

  /**
   * Persist an instance of the provided entity in the database.
   *
   * @param entity entity to persist
   * @return returns the original entity.
   */
  public E create(E entity) {
    preCreate(entity);
    baseDAO.create(entity);
    return entity;
  }

  /**
   * Merge the state of a given entity into the current persistence context.
   *
   * @param entity entity to update
   * @return returns the managed instance the state was merged to.
   */
  public E update(E entity) {
    preUpdate(entity);
    return baseDAO.update(entity);
  }

  /**
   * Remove the given entity from the database.
   *
   * @param entity entity to delete
   */
  public void delete(E entity) {
    preDelete(entity);
    baseDAO.delete(entity);
  }

  /**
   * <p>
   * Returns a list of Entities of a given class where the fields match a given
   * map.
   * <p>
   *
   * <p>
   * Given where map maps field names to values. Where map can be empty to find
   * all. Where map can search on null values.
   * <p>
   * 
   * @param entityClass - entity class to search on
   * @param where       - map of fieldName::Values to match on
   * @return list of Entities
   */
  public List<E> findAllWhere(@NonNull Class<E> entityClass, @NonNull Map<String, Object> where) {
    return findAllByPredicates(entityClass, (cb, r) -> {
      return where.entrySet().stream().map(entry -> {
        if (entry.getValue() == null) {
          return cb.isNull(r.get(entry.getKey()));
        }
        return cb.equal(r.get(entry.getKey()), entry.getValue());
      }).toArray(Predicate[]::new);
    }, null, 0, 100);
  }

  /**
   * Returns a list of Entities of a given class restricted by the predicates
   * returned by a given function.
   *
   * @param entityClass
   *                      - entity class to query cannot be null
   * @param func
   *                      - function to return the predicates cannot be null
   * @param orderBy
   *                      - function to return the sorting criteria can be null
   * @param startIndex
   *                      - position of first result to retrieve
   * @param maxResult
   *                      - maximun number of results to return
   * @return list of entities
   */
  public List<E> findAllByPredicates(
    @NonNull Class<E> entityClass,
    @NonNull BiFunction<CriteriaBuilder, Root<E>, Predicate[]> func,
    BiFunction<CriteriaBuilder, Root<E>, List<Order>> orderBy,
    int startIndex,
    int maxResult
  ) {
    CriteriaBuilder criteriaBuilder = baseDAO.getCriteriaBuilder();
    CriteriaQuery<E> criteria = criteriaBuilder.createQuery(entityClass);
    Root<E> root = criteria.from(entityClass);

    criteria.where(func.apply(criteriaBuilder, root)).select(root);
    if (orderBy != null) {
      criteria.orderBy(orderBy.apply(criteriaBuilder, root));
    }
    return baseDAO.resultListFromCriteria(criteria, startIndex, maxResult);
  }

  /**
   * Returns the resource count from a given predicate supplier.
   * 
   * @param entityClass
   *                            - entity class to query cannot be null
   * @param predicateSupplier
   *                            - function to return the predicates cannot be null
   * @return resource count
   */
  public Long getResourceCount(
    @NonNull Class<E> entityClass,
    @NonNull BiFunction<CriteriaBuilder, Root<E>, Predicate[]> predicateSupplier
  ) {
    return baseDAO.getResourceCount(entityClass, predicateSupplier);
  }

  /**
   * Find an entity by it's NaturalId. The method assumes that the naturalId is
   * unique.
   * 
   * @param naturalId   - id of entity
   * @param entityClass - class of entity
   * @return the matched entity
   */
  public <T> T findOne(Object naturalId, Class<T> entityClass) {
    return baseDAO.findOneByNaturalId(naturalId, entityClass);
  }

  /**
   * Returns a reference to an entity that should exist without actually loading
   * it. Useful to set relationships without loading the entity instead of findOne.
   * 
   * @param naturalId   - natural id of entity
   * @param entityClass - class of entity
   * @return the matched reference
   */
  public <T> T findOneReferenceByNaturalId(Class<T> entityClass, Object naturalId) {
    return baseDAO.getReferenceByNaturalId(entityClass, naturalId);
  }

  /**
   * Returns a new instance of a {@link JpaCriteriaQueryFactory} for crnk JPA data
   * access.
   */
  public JpaCriteriaQueryFactory createJpaCritFactory() {
    return baseDAO.createWithEntityManager(JpaCriteriaQueryFactory::newInstance);
  }

  /**
   * Run before the {@link DinaService#create()} method.
   *
   * @param entity entity being created by {@link DinaService#create()}
   * @return returns the created entity.
   */
  protected abstract E preCreate(E entity);

  /**
   * Run before the {@link DinaService#update()} method.
   *
   * @param entity entity being updated by {@link DinaService#update()}
   * @return returns the updated entity.
   */
  protected abstract E preUpdate(E entity);

  /**
   * Run before the {@link DinaService#delete()} method.
   *
   * @param entity entity being deleted by {@link DinaService#delete()}
   */
  protected abstract void preDelete(E entity);

}
