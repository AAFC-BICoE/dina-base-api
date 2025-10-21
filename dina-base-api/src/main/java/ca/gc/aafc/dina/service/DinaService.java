package ca.gc.aafc.dina.service;

import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.jpa.PredicateSupplier;

import java.util.Map;
import java.util.function.Consumer;
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

  <T> void setRelationshipByNaturalIdReference(Class<T> entityClass, Object naturalId, Consumer<T> objConsumer);

  /**
   * Deletes a given entity from the data source
   *
   * @param entity entity to delete.
   */
  void delete(E entity);

  /**
   * Find an entity by its NaturalId. The method assumes that the naturalId is unique.
   *
   * @param naturalId   - id of entity
   * @param entityClass - class of entity
   * @return the matched entity or null if not found
   */
  <T> T findOne(Object naturalId, Class<T> entityClass);

  /**
   * Find an entity by its NaturalId. The method assumes that the naturalId is unique.
   * The relationships set can be used to force lazy loaded relationships to be loaded.
   * @param naturalId
   * @param entityClass
   * @param relationships relationships to load or an empty set, not null.
   * @return the matched entity or null if not found
   */
  <T> T findOne(Object naturalId, Class<T> entityClass, Set<String> relationships);

  /**
   * Called after findOne or findAll to load optional fields
   * @param entity
   * @param optionalFields optionalFields optional field to load (by type)
   * @return provided entity with optional field loaded (if supported)
   */
  E handleOptionalFields(E entity, Map<String, List<String>> optionalFields);

  /**
   * Used to compute or provide additional data after the entity is loaded.
   * @param entity
   * @param relationships
   */
  void augmentEntity(E entity, Set<String> relationships);

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
   * @param includes - list of includes including but not limited to relationships or an empty set, not null.
   * @param relationships - relationships to load or an empty set, not null.
   * @return list of entities
   */
  <T> List<T> findAll(
    @NonNull Class<T> entityClass,
    @NonNull PredicateSupplier<T> where,
    BiFunction<CriteriaBuilder, Root<T>, List<Order>> orderBy,
    int startIndex,
    int maxResult,
    @NonNull Set<String> includes,
    @NonNull Set<String> relationships
  );

  <T> List<T> findAll(
    Class<T> entityClass,
    String fiql,
    List<String> orderBy,
    int startIndex,
    int maxResult,
    @NonNull Set<String> includes,
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
   * Returns the resource count from a FIQL string
   * @param entityClass
   * @param fiql
   * @return resource count
   */
  <T> Long getResourceCount(
    @NonNull Class<T> entityClass,
    @NonNull String fiql
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
