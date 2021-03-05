package ca.gc.aafc.dina.jpa;

import ca.gc.aafc.dina.service.DinaService;
import io.crnk.core.engine.information.bean.BeanInformation;
import lombok.NonNull;
import org.hibernate.Session;
import org.hibernate.SimpleNaturalIdLoadAccess;
import org.hibernate.annotations.NaturalId;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnitUtil;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Base Data Access Object layer. This class should be the only one holding a
 * reference to the {@link EntityManager}.
 *
 */
@Component
public class BaseDAO {

  public static final int DEFAULT_LIMIT = 100;

  @PersistenceContext
  private EntityManager entityManager;

  @Inject
  private Validator validator;

  /**
   * This method can be used to inject the EntityManager into an external object.
   *
   * @param creator
   */
  public <T> T createWithEntityManager(Function<EntityManager, T> creator) {
    Objects.requireNonNull(creator);
    return creator.apply(entityManager);
  }

  /**
   * Utility function that can check if a lazy loaded attribute is actually
   * loaded.
   *
   * @param entity
   * @param fieldName
   * @return
   */
  public Boolean isLoaded(Object entity, String fieldName) {
    Objects.requireNonNull(entity);
    Objects.requireNonNull(fieldName);

    PersistenceUnitUtil unitUtil = entityManager.getEntityManagerFactory().getPersistenceUnitUtil();
    return unitUtil.isLoaded(entity, fieldName);
  }

  /**
   * Find an entity by it's natural ID or database ID. The method assumes that the
   * naturalId is unique.
   *
   * @param id
   * @param entityClass
   * @return
   */
  public <T> T findOneByDatabaseId(Object id, Class<T> entityClass) {
    return entityManager.find(entityClass, id);
  }

  /**
   * Find an entity by it's {@link NaturalId}. The method assumes that the
   * naturalId is unique.
   *
   * @param id
   * @param entityClass
   * @return
   */
  public <T> T findOneByNaturalId(Object id, Class<T> entityClass) {
    Session session = entityManager.unwrap(Session.class);
    return session.bySimpleNaturalId(entityClass).load(id);
  }

  /**
   * Find an entity by a specific property. The method assumes that the property
   * is unique.
   *
   * @param clazz
   * @param property
   * @param value
   * @return the entity or null if not found
   */
  public <T> T findOneByProperty(Class<T> clazz, String property, Object value) {
    // Create a criteria to retrieve the specific property.
    CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
    CriteriaQuery<T> criteria = criteriaBuilder.createQuery(clazz);
    Root<T> root = criteria.from(clazz);

    criteria.where(criteriaBuilder.equal(root.get(property), value));
    criteria.select(root);

    TypedQuery<T> query = entityManager.createQuery(criteria);
    try {
      return query.getSingleResult();
    } catch (NoResultException nrEx) {
      return null;
    }
  }

  /**
   * Find one or more entity by a specific property. The number of records returned is limited
   * to {@link #DEFAULT_LIMIT}.
   *
   * @param clazz
   * @param property
   * @param value
   * @return list of entities or empty list if nothing is found
   */
  public <T> List<T> findByProperty(Class<T> clazz, String property, Object value) {
    // Create a criteria to retrieve the specific property.
    CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
    CriteriaQuery<T> criteria = criteriaBuilder.createQuery(clazz);
    Root<T> root = criteria.from(clazz);

    criteria.where(criteriaBuilder.equal(root.get(property), value));
    criteria.select(root);

    TypedQuery<T> query = entityManager.createQuery(criteria);
    return query.setMaxResults(DEFAULT_LIMIT).getResultList();
  }

  /**
   * Check for the existence of a record based on a property and a value
   *
   * @param clazz
   * @param property
   * @param value
   * @param <T>
   * @return
   */
  public <T> boolean existsByProperty(Class<T> clazz, String property, Object value) {
    CriteriaBuilder cb = entityManager.getCriteriaBuilder();
    CriteriaQuery<Integer> cq = cb.createQuery(Integer.class);
    Root<T> from = cq.from(clazz);

    cq.select(cb.literal(1))
      .where(
        cb.equal(
          from.get(property),
            value))
        .from(clazz);

    TypedQuery<Integer> tq = entityManager.createQuery(cq);
    return !tq.getResultList().isEmpty();
  }

  /**
   * Check for the existence of a record by natural id.
   *
   * @param naturalId
   * @param entityClass
   * @return
   */
  public <T> boolean existsByNaturalId(
      @NonNull Object naturalId,
      @NonNull Class<T> entityClass
  ) {
    return existsByProperty(entityClass, getNaturalIdFieldName(entityClass), naturalId);
  }

  /**
   * Returns a reference to an entity that should exist without actually loading it. Useful to set
   * relationships without loading the entity.
   *
   * @param entityClass
   * @param naturalId
   * @return
   */
  public <T> T getReferenceByNaturalId(Class<T> entityClass, Object naturalId) {
    SimpleNaturalIdLoadAccess<T> loadAccess = entityManager.unwrap(Session.class)
        .bySimpleNaturalId(entityClass);
    return loadAccess.getReference(naturalId);
  }

  /**
   * Set a relationship by calling the provided {@link Consumer} with a reference Entity loaded by
   * NaturalId. A reference to the entity allows to set a foreign key without loading the other entity.
   *
   * Usage:
   *
   * Using the object 'dep', set the relationship to DepartmentType using only its NaturalId (depTypeUUID).
   * baseDAO.setRelationshipByNaturalIdReference(DepartmentType.class, depTypeUUID,
        (x) -> dep.setDepartmentType(x));
   *
   * @param entityClass entity to link to that will be loaded with a reference entity
   * @param naturalId value
   * @param objConsumer
   */
  public <T> void setRelationshipByNaturalIdReference(Class<T> entityClass, Object naturalId, Consumer<T> objConsumer) {
    objConsumer.accept(getReferenceByNaturalId(entityClass, naturalId));
  }

  /**
   * Save the provided entity.
   *
   * @param entity
   */
  public void create(Object entity) {
    entityManager.persist(entity);
  }

  /**
   * Merge the state of a given entity into the current persistence context.
   *
   * @param <E>    Type of the entity
   * @param entity entity to update
   * @return returns the managed instance the state was merged to.
   */
  public <E> E update(E entity) {
    E result = entityManager.merge(entity);
    // Flush here to throw any validation errors:
    entityManager.flush();
    return result;
  }

  /**
   * Delete the provided entity.
   *
   * @param entity
   */
  public void delete(Object entity) {
    entityManager.remove(entity);
  }

  /**
   * Same as {@link Validator#validate(Object, Class...)}
   *
   * @param entity
   *          the entity to validate (not null)
   * @return constraint violations or an empty set if none
   */
  public <T> Set<ConstraintViolation<T>> validateEntity(T entity) {
    return validator.validate(entity);
  }

  /**
   * Given a class, this method will extract the name of the field annotated with {@link NaturalId}.
   *
   * @param entityClass
   * @return
   */
  public String getNaturalIdFieldName(Class<?> entityClass) {
    BeanInformation beanInfo = BeanInformation.get(entityClass);
    // Check for NaturalId:
    for (String attrName : beanInfo.getAttributeNames()) {
      if (beanInfo.getAttribute(attrName).getAnnotation(NaturalId.class).isPresent()) {
        return attrName;
      }
    }
    return null;
  }

  /**
   * Given a class, this method will return the name of the field annotated with {@link Id}.
   *
   * @param entityClass
   * @return
   */
  public String getDatabaseIdFieldName(Class<?> entityClass) {
    return entityManager.getMetamodel()
        .entity(entityClass)
        .getId(Serializable.class)
        .getName();
  }

  /**
   * returns a {@link CriteriaBuilder} for the creation of {@link CriteriaQuery},
   * {@link Predicate}, {@link Expression}, and compound selections.
   *
   * @return {@link CriteriaBuilder}
   */
  public CriteriaBuilder getCriteriaBuilder() {
    return entityManager.getCriteriaBuilder();
  }

  /**
   * Returns a List of entities based off a given criteria.
   *
   * @param <E>
   *                    - Type of result list
   * @param criteria
   *                    - criteria to generate the typed query
   * @param start
   *                    - position of first result to retrieve
   * @param maxResult
   *                    - maximun number of results to return
   * @return List of entities
   */
  public <E> List<E> resultListFromCriteria(CriteriaQuery<E> criteria, int start, int maxResult) {
    return entityManager.createQuery(criteria)
      .setFirstResult(start)
      .setMaxResults(maxResult)
      .getResultList();
  }

  /**
   * Returns the resource count from a given predicate supplier.
   *
   * @param <E>               entity type
   * @param entityClass       - entity class to query cannot be null
   * @param predicateSupplier - function to return the predicates cannot be null
   * @return resource count
   */
  public <E> Long getResourceCount(
    @NonNull Class<E> entityClass,
    @NonNull DinaService.DinaPredicateSupplier<E> predicateSupplier
  ) {
    CriteriaBuilder cb = entityManager.getCriteriaBuilder();
    CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
    Root<E> root = countQuery.from(entityClass);
    countQuery.select(cb.count(root));
    countQuery.where(predicateSupplier.supply(cb, root, createWithEntityManager(m -> m)));
    return entityManager.createQuery(countQuery).getSingleResult();
  }
}
