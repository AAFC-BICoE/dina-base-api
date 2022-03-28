package ca.gc.aafc.dina.jpa;

import io.crnk.core.engine.information.bean.BeanInformation;
import lombok.NonNull;

import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.Session;
import org.hibernate.SimpleNaturalIdLoadAccess;
import org.hibernate.annotations.NaturalId;
import org.springframework.stereotype.Component;

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
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
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
   * Used to call the provided PredicateSupplier with the EntityManager.
   * @param where
   * @param criteriaBuilder
   * @param root
   * @param <T>
   * @return
   */
  public <T> Predicate[] buildPredicateFromSupplier(PredicateSupplier<T> where, CriteriaBuilder criteriaBuilder, Root<T> root) {
    return where.supply(criteriaBuilder, root, entityManager);
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
   * Find an entity by its primary key.
   *
   * @param id
   * @param entityClass
   * @return
   */
  public <T> T findOneByDatabaseId(Object id, Class<T> entityClass) {
    return entityManager.find(entityClass, id);
  }

  /**
   * Find an entity by its {@link NaturalId}. The method assumes that the
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
   * Find an entity by specific properties. The method assumes that the properties
   * are part of a unique constraint (that the query will return nothing or 1 result but never more).
   * @param clazz
   * @param propertiesAndValue
   * @param <T>
   * @return
   */
  public <T> T findOneByProperties(Class<T> clazz, List<Pair<String, Object>> propertiesAndValue) {
    // Create a criteria to retrieve the specific property.
    CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
    CriteriaQuery<T> criteria = criteriaBuilder.createQuery(clazz);
    Root<T> root = criteria.from(clazz);

    Predicate whereClause =
        PredicateHelper.appendPropertiesEqual(null, criteriaBuilder, root, propertiesAndValue);

    criteria.where(whereClause);
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
    create(entity, false);
  }

  /**
   * Save the provided entity.
   *
   * @param entity
   * @param flush should the changes made in the current transaction be flushed immediately to the database
   */
  public void create(Object entity, boolean flush) {
    entityManager.persist(entity);
    if(flush) {
      entityManager.flush();
    }
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
   * Refresh an entity from the database.
   * This will revert any non-flushed changes made in the current transaction to the entity, and refresh its state to what is currently defined on the database.
   * @param entity
   */
  public void refresh(Object entity) {
    entityManager.refresh(entity);
  }

  /**
   * Force a flush to the database.
   * This is usually not necessary unless it is important to flush changes in the current transaction
   * at a very specific moment. Otherwise, the transaction will automatically flush at the end.
   */
  public void flush() {
    entityManager.flush();
  }

  /**
   * Remove the given entity from the persistence context, causing a managed entity to become detached.
   * This will revert any non-flushed changes made in the current transaction to the entity.
   *
   * Also used to remove the object from the first-level cache.
   * @param entity
   */
  public void detach(Object entity) {
    entityManager.detach(entity);
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
    @NonNull PredicateSupplier<E> predicateSupplier
  ) {
    CriteriaBuilder cb = entityManager.getCriteriaBuilder();
    CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
    Root<E> root = countQuery.from(entityClass);
    countQuery.select(cb.count(root));
    countQuery.where(predicateSupplier.supply(cb, root, entityManager));
    return entityManager.createQuery(countQuery).getSingleResult();
  }
}
