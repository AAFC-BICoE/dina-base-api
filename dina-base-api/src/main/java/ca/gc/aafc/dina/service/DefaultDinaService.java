package ca.gc.aafc.dina.service;

import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.jpa.BaseDAO;
import ca.gc.aafc.dina.jpa.PredicateSupplier;
import ca.gc.aafc.dina.validation.ValidationErrorsHelper;

import java.util.function.Consumer;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;
import org.springframework.validation.SmartValidator;
import org.springframework.validation.Validator;

import javax.inject.Inject;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.ValidationException;
import javax.validation.groups.Default;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

  @NonNull
  private final SmartValidator validator;

  /**
   * Persist an instance of the provided entity in the database.
   *
   * @param entity entity to persist
   * @return returns the original entity.
   */
  @Override
  public E create(E entity) {
    return create(entity, false);
  }

  /**
   * Persist an instance of the provided entity and flush the content of the current transaction
   * to the database.
   *
   * @param entity entity to persist
   * @return returns the original entity.
   */
  public E createAndFlush(E entity) {
    return create(entity, true);
  }

  /**
   * Private method handling entity create
   * @param entity
   * @param flush
   * @return
   */
  private E create(E entity, boolean flush) {
    preCreate(entity);
    validateConstraints(entity, OnCreate.class);
    validateBusinessRules(entity);
    baseDAO.create(entity, flush);
    postCreate(entity);
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
    validateConstraints(entity, OnUpdate.class);
    validateBusinessRules(entity);
    return baseDAO.update(entity);
  }

  @Override
  public <T> void setRelationshipByNaturalIdReference(Class<T> entityClass, Object naturalId, Consumer<T> objConsumer) {
    baseDAO.setRelationshipByNaturalIdReference(entityClass, naturalId, objConsumer);
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
   * Returns a list of Entities of a given class restricted by the predicates returned by a given function.
   *
   * @param entityClass - entity class to query cannot be null
   * @param where       - function to return the predicates cannot be null
   * @param orderBy     - function to return the sorting criteria can be null
   * @param startIndex  - position of first result to retrieve
   * @param maxResult   - maximum number of results to return
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
    return findAll(entityClass, (criteriaBuilder, root, em) -> where.apply(criteriaBuilder, root),
      orderBy, startIndex, maxResult, Set.of(), Set.of());
  }

  /**
   * See {@link BaseDAO#refresh(Object)}
   */
  public void refresh(Object entity) {
    baseDAO.refresh(entity);
  }

  /**
   * See {@link BaseDAO#detach(Object)}
   */
  public void detach(Object entity) {
    baseDAO.detach(entity);
  }

  /**
   * See {@link BaseDAO#flush()}
   */
  public void flush() {
    baseDAO.flush();
  }


  @Override
  public <T> List<T> findAll(
    @NonNull Class<T> entityClass,
    @NonNull PredicateSupplier<T> where,
    BiFunction<CriteriaBuilder, Root<T>, List<Order>> orderBy,
    int startIndex,
    int maxResult,
    @NonNull Set<String> includes,
    @NonNull Set<String> relationships
  ) {
    CriteriaBuilder criteriaBuilder = baseDAO.getCriteriaBuilder();
    CriteriaQuery<T> criteria = criteriaBuilder.createQuery(entityClass);
    Root<T> root = criteria.from(entityClass);
    Predicate[] predicates = baseDAO.buildPredicateFromSupplier(where, criteriaBuilder, root);

    if (ArrayUtils.isNotEmpty(predicates)) {
      criteria.where(predicates);
    }
    criteria.select(root);
    if (orderBy != null) {
      criteria.orderBy(orderBy.apply(criteriaBuilder, root));
    }

    Map<String, Object> hints = relationships.isEmpty() ? null : relationshipPathToLoadHints(entityClass, relationships);
    return baseDAO.resultListFromCriteria(criteria, startIndex, maxResult, hints);
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
    @NonNull PredicateSupplier<T> predicateSupplier
  ) {
    return baseDAO.getResourceCount(entityClass, predicateSupplier);
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
    return getResourceCount(
      entityClass,
      (criteriaBuilder, root, em) -> predicateSupplier.apply(criteriaBuilder, root));
  }


  @Override
  public <T> T findOne(Object naturalId, Class<T> entityClass) {
    return baseDAO.findOneByNaturalId(naturalId, entityClass);
  }

  @Override
  public <T> T findOne(Object naturalId, Class<T> entityClass, Set<String> relationships) {
    Map<String, Object> hints = relationships.isEmpty() ? null : relationshipPathToLoadHints(entityClass, relationships);
    return baseDAO.findOneByNaturalId(naturalId, entityClass, hints);
  }

  /**
   * Find an entity by its database id.
   *
   * @param id   - id of entity
   * @param entityClass - class of entity
   * @return the matched entity
   */
  public <T> T findOneById(Object id, Class<T> entityClass) {
    return baseDAO.findOneByDatabaseId(id, entityClass);
  }

  /**
   * Find an entity by a specific property.
   *
   * @param clazz
   * @param property
   * @param value
   * @return the entity or null if not found
   */
  public E findOneByProperty(Class<E> clazz, String property, Object value) {
    return baseDAO.findOneByProperty(clazz, property, value);
  }

  /**
   * Find an entity by a specific properties.
   * The combination of the properties are assumed to be unique and returned only 1 entity.
   *
   * @param clazz
   * @param propertiesAndValue
   * @return the entity or null if not found
   */
  public E findOneByProperties(Class<E> clazz,List<Pair<String, Object>> propertiesAndValue) {
    return baseDAO.findOneByProperties(clazz, propertiesAndValue);
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
   * Run after the {@link DefaultDinaService#create(DinaEntity)} method.
   *
   * @param entity entity created by {@link DefaultDinaService#create(DinaEntity)}
   */
  protected void postCreate(E entity) {
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

  /**
   * Check for the existence of a record based on a property and a value
   * 
   * @param clazz
   * @param property
   * @param value
   * @return
   */
  public boolean existsByProperty(Class<E> clazz, String property, Object value) {
    return baseDAO.existsByProperty(clazz, property, value);
  }

  /**
   * Find a list of entities by a specific property.
   * 
   * @param clazz
   * @param property
   * @param value
   * @return the entity or null if not found
   */
  public List<E> findByProperty(Class<E> clazz, String property, Object value) {
    return baseDAO.findByProperty(clazz, property, value);
  }

  /**
   * Function that validates an entity against a specific validator to check business rules.
   * @param entity
   * @param validator business rules validator
   * @throws ValidationException if the validator returned an error
   */
  protected void applyBusinessRule(E entity, Validator validator) {
    Objects.requireNonNull(entity);
    applyBusinessRule(entity, validator, ValidationErrorsHelper.newErrorsObject(entity));
  }

  /**
   * Function that validates an object against a specific validator to check business rules.
   * This function should be used to validate objects that are not {@link DinaEntity}.
   * @param objIdentifier
   * @param target
   * @param validator
   */
  protected static void applyBusinessRule(String objIdentifier, Object target, Validator validator) {
    Objects.requireNonNull(target);
    applyBusinessRule(target, validator, ValidationErrorsHelper.newErrorsObject(objIdentifier, target));
  }

  private static void applyBusinessRule(Object target, Validator validator, Errors errors) {
    Objects.requireNonNull(target);
    Objects.requireNonNull(errors);

    validator.validate(target, errors);
    ValidationErrorsHelper.errorsToValidationException(errors);
  }

  @Override
  public void validateBusinessRules(E entity) {
  }

  private <T> Map<String, Object> relationshipPathToLoadHints(Class<T> clazz, Set<String> rel) {
    if (rel.isEmpty() ) {
      return Map.of();
    }
    return Map.of(BaseDAO.LOAD_GRAPH_HINT_KEY, baseDAO.createEntityGraph(clazz, rel.toArray(new String[0])));
  }

  @SuppressWarnings("unchecked")
  @Override
  public void validateConstraints(E entity, Class<? extends Default> validationGroup) {
    Errors errors = ValidationErrorsHelper.newErrorsObject(entity);

    validator.validate(entity, errors, validationGroup);

    if (errors.hasErrors()) {
      Set<ConstraintViolation<E>> violations = new HashSet<>();
      for (ObjectError o : errors.getAllErrors()) {
        if (o.contains(ConstraintViolation.class)) { 
          violations.add((ConstraintViolation<E>) o.unwrap(ConstraintViolation.class));
        }
      }
      throw new ConstraintViolationException(violations);
    }
  }

  /**
   * Optional group standardization method.
   *
   * @param entity
   * @return standardized group name (lowercase)
   */
  public String standardizeGroupName(E entity) {
    if (entity.getGroup() == null) {
      return null;
    }
    return entity.getGroup().toLowerCase();
  }

}
