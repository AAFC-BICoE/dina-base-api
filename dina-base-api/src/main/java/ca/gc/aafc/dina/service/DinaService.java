package ca.gc.aafc.dina.service;

import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.jpa.PredicateSupplier;
import lombok.NonNull;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.validation.groups.Default;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * Service class to provide a interface to a datasource.
 *
 * @param <E> - Type of {@link DinaEntity}
 */
public interface DinaService<E extends DinaEntity> {

  /**
   * Creates and returns a given entity with is newly assigned id. the returned Entity should be returned in
   * the state it was persisted.
   *
   * @param entity entity to create.
   * @return a given entity with is newly assigned id.
   */
  E create(E entity);

  /**
   * Updates and returns a given entity as it was persisted.
   *
   * @param entity entity to update.
   * @return a given entity as it was persisted.
   */
  E update(E entity);

  /**
   * Deletes a given entity from the data source
   *
   * @param entity entity to delete.
   */
  void delete(E entity);

  /**
   * Find an entity by it's NaturalId. The method assumes that the naturalId is unique.
   *
   * @param naturalId   - id of entity
   * @param entityClass - class of entity
   * @return the matched entity
   */
  <T> T findOne(Object naturalId, Class<T> entityClass);

  /**
   * Returns a reference to an entity that should exist without actually loading it. Useful to set
   * relationships without loading the entity instead of findOne.
   *
   * @param naturalId   - natural id of entity
   * @param entityClass - class of entity
   * @return the matched reference
   */
  <T> T getReferenceByNaturalId(Class<T> entityClass, Object naturalId);

  /**
   * Returns a list of Entities of a given class restricted by the predicates returned by a given function.
   *
   * @param entityClass - entity class to query cannot be null
   * @param where       - function to return the predicates cannot be null
   * @param orderBy     - function to return the sorting criteria can be null
   * @param startIndex  - position of first result to retrieve
   * @param maxResult   - maximum number of results to return
   * @return list of entities
   */
  <T> List<T> findAll(
    @NonNull Class<T> entityClass,
    @NonNull BiFunction<CriteriaBuilder, Root<T>, Predicate[]> where,
    BiFunction<CriteriaBuilder, Root<T>, List<Order>> orderBy,
    int startIndex,
    int maxResult
  );

  /**
   * Returns a list of Entities of a given class restricted by the predicates returned by a given function.
   *
   * @param entityClass - entity class to query cannot be null
   * @param where       - function to return the predicates cannot be null
   * @param orderBy     - function to return the sorting criteria can be null
   * @param startIndex  - position of first result to retrieve
   * @param maxResult   - maximum number of results to return
   * @param relationships - relationships to load or an empty set, not null.
   * @return list of entities
   */
  <T> List<T> findAll(
    @NonNull Class<T> entityClass,
    @NonNull PredicateSupplier<T> where,
    BiFunction<CriteriaBuilder, Root<T>, List<Order>> orderBy,
    int startIndex,
    int maxResult,
    @NonNull Set<String> relationships
  );

  /**
   * Returns the resource count from a given predicate supplier.
   *
   * @param entityClass       - entity class to query cannot be null
   * @param predicateSupplier - function to return the predicates cannot be null
   * @return resource count
   */
  <T> Long getResourceCount(
    @NonNull Class<T> entityClass,
    @NonNull BiFunction<CriteriaBuilder, Root<T>, Predicate[]> predicateSupplier
  );

  /**
   * Returns the resource count from a given predicate supplier.
   *
   * @param entityClass       - entity class to query cannot be null
   * @param predicateSupplier - function to return the predicates cannot be null
   * @return resource count
   */
  <T> Long getResourceCount(
    @NonNull Class<T> entityClass,
    @NonNull PredicateSupplier<T> predicateSupplier
  );

  /**
   * Check for the existence of a record by natural id.
   */
  boolean exists(Class<?> entityClass, Object naturalId);

  void validateConstraints(E entity, Class<? extends Default> validationGroup);

  /**
   * Function that validates an entity against all the business rules defined by a concrete
   * service.
   * @param entity
   */
  void validateBusinessRules(E entity);

}
