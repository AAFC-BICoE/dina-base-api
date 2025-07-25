package ca.gc.aafc.dina.jpa;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import lombok.NonNull;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.Session;
import org.hibernate.SimpleNaturalIdLoadAccess;
import org.hibernate.annotations.NaturalId;
import org.hibernate.jpa.QueryHints;
import org.springframework.stereotype.Component;

import javax.persistence.EntityGraph;
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
import java.util.Map;
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

  public static final String LOAD_GRAPH_HINT_KEY = "javax.persistence.loadgraph";
  public static final int DEFAULT_STREAM_FETCH_SIZE = 100;

  public static final int DEFAULT_LIMIT = 100;

  private static final Map<Class<?>, String> NATURAL_ID_CACHE = new ConcurrentHashMap<>();
  
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
   * Creates an EntityGraph for the specified entity class with the given attribute nodes.
   *
   * This method constructs an EntityGraph for the provided entity class and populates it
   * with the specified attribute nodes. The attribute nodes can represent either single-level 
   * attributes or nested attributes in a dot-separated format. If an attribute node contains a dot, 
   * it will be treated as a nested attribute, and a subgraph will be created.
   *
   * @param entityClass     The class of the entity for which the EntityGraph is constructed.
   * @param attributeNodes  The attribute nodes to be included in the EntityGraph.
   *                        The attribute nodes can be either single-level attributes or nested attributes
   *                        represented in a dot-separated format (e.g., "attributeName" or "nestedEntity.attributeName").
   * @return An EntityGraph for the specified entity class with the given attribute nodes.
   */
  public <T> EntityGraph<T> createEntityGraph(Class<T> entityClass, String... attributeNodes) {
    EntityGraph<T> graph = entityManager.createEntityGraph(entityClass);
    for (String attribute : attributeNodes) {
      if (attribute.contains(".")) {
        String[] parts = StringUtils.split(attribute, ".", 2);

        if (parts.length == 2) {
          graph.addSubgraph(parts[0]).addAttributeNodes(parts[1]);
        }
      } else {
        graph.addAttributeNodes(attribute);
      }
    }
    return graph;
  }

  /**
   * Find a POJO/scalar(class that is not necessary an entity) Projection from a query.
   * @param typeClass class of the result
   * @param sql sql query. Usually a jpql query.
   * @param parameters optional parameters for the query
   * @return the POJO/Scalar or null if not found
   */
  public <T> T findOneByQuery(Class<T> typeClass, String sql,
                              List<Pair<String, Object>> parameters) {
    TypedQuery<T> tq = entityManager.createQuery(sql, typeClass);
    if (parameters != null) {
      for (Pair<String, Object> param : parameters) {
        tq.setParameter(param.getKey(), param.getValue());
      }
    }
    try {
      return tq.getSingleResult();
    } catch (NoResultException nrEx) {
      return null;
    }
  }

  /**
   * Find a list of POJO/scalar(class that is not necessary an entity) Projection from a query.
   * @param typeClass class of the result
   * @param sql sql query. Usually a jpql query.
   * @param parameters optional parameters for the query
   * @return the list of POJO/Scalar or null if not found
   */
  public <T> List<T> findAllByQuery(Class<T> typeClass, String sql,
                              List<Pair<String, Object>> parameters) {
    return findAllByQuery(typeClass, sql, parameters, -1, -1);
  }

  /**
   * Find a list of POJO/scalar(class that is not necessary an entity) Projection from a query.
   * @param typeClass class of the result
   * @param sql sql query. Usually a jpql query.
   * @param parameters optional parameters for the query
   * @param limit optional parameters to limit the page size.
   * @param offset optional parameters to set the page offset. If used make sure the query includes an ORDER by.
   * @return the list of POJO/Scalar or null if nothing found
   */
  public <T> List<T> findAllByQuery(Class<T> typeClass, String sql,
                                    List<Pair<String, Object>> parameters, int limit, int offset) {
    TypedQuery<T> tq = entityManager.createQuery(sql, typeClass);
    if (parameters != null) {
      for (Pair<String, Object> param : parameters) {
        tq.setParameter(param.getKey(), param.getValue());
      }
    }

    // greater than 10 000 is stream should be used
    if (limit > 0 && limit < 10_000) {
      tq.setMaxResults(limit);
    }

    if (offset > 0) {
      tq.setFirstResult(offset);
    }

    try {
      return tq.getResultList();
    } catch (NoResultException nrEx) {
      return null;
    }
  }

  /**
   * Stream of POJO/scalar(class that is not necessary an entity) Projection from a query.
   * @param typeClass class of the result
   * @param sql sql query. Usually a jpql query.
   * @param parameters optional parameters for the query
   * @return the Stream of POJO/Scalar or empty stream if not found (never null)
   */
  public <T> Stream<T> streamAllByQuery(Class<T> typeClass, String sql,
                                        List<Pair<String, Object>> parameters) {

    TypedQuery<T> tq = entityManager.createQuery(sql, typeClass);
    if (parameters != null) {
      for (Pair<String, Object> param : parameters) {
        tq.setParameter(param.getKey(), param.getValue());
      }
    }
    tq.setHint(QueryHints.HINT_FETCH_SIZE, DEFAULT_STREAM_FETCH_SIZE);
    try {
      return tq.getResultStream();
    } catch (NoResultException nrEx) {
      return Stream.empty();
    }
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
   * Find an entity by its {@link NaturalId}. The method assumes that the
   * naturalId is unique.
   *
   * @param id
   * @param entityClass
   * @param hints
   * @return
   */
  public <T> T findOneByNaturalId(Object id, Class<T> entityClass, Map<String, Object> hints) {

    // Hibernate 5 doesn't support hint on natural id, so we are creating a real query
    CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
    CriteriaQuery<T> criteria = criteriaBuilder.createQuery(entityClass);
    Root<T> root = criteria.from(entityClass);

    criteria.where(criteriaBuilder.equal(root.get(getNaturalIdFieldName(entityClass)), id));
    criteria.select(root);

    TypedQuery<T> query = entityManager.createQuery(criteria);
    if (hints != null) {
      hints.forEach(query::setHint);
    }
    try {
      return query.getSingleResult();
    } catch (NoResultException nrEx) {
      return null;
    }
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
    if (flush) {
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
   * Given a class, this method will extract the name of the field/getter annotated with {@link NaturalId}.
   *
   * @param entityClass
   * @return the name of the field
   */
  public String getNaturalIdFieldName(Class<?> entityClass) {

    // Check cache first
    if (NATURAL_ID_CACHE.containsKey(entityClass)) {
      return NATURAL_ID_CACHE.get(entityClass);
    }

    for (Field field : FieldUtils.getAllFields(entityClass)) {
      for (Annotation annotation : field.getDeclaredAnnotations()) {
        if (annotation.annotationType() == NaturalId.class) {
          NATURAL_ID_CACHE.put(entityClass, field.getName());
          return field.getName();
        }
      }
    }

    // Maybe the annotation is on the getter
    List<Method> naturalIdMethod =
      MethodUtils.getMethodsListWithAnnotation(entityClass, NaturalId.class, true, false);
    if (!naturalIdMethod.isEmpty()) {
      PropertyDescriptor[] descriptors = PropertyUtils.getPropertyDescriptors(entityClass);
      for (PropertyDescriptor descriptor : descriptors) {
        if (descriptor.getReadMethod() != null &&
          descriptor.getReadMethod().equals(naturalIdMethod.getFirst())) {
          NATURAL_ID_CACHE.put(entityClass, descriptor.getName());
          return descriptor.getName();
        }
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
   *                    - maximum number of results to return
   * @return List of entities
   */
  public <E> List<E> resultListFromCriteria(CriteriaQuery<E> criteria, int start, int maxResult) {
    return resultListFromCriteria(criteria, start, maxResult, null);
  }

  /**
   * Returns a List of entities based off a given criteria and a Hibernate hint.
   *
   * @param <E>
   *                    - Type of result list
   * @param criteria
   *                    - criteria to generate the typed query
   * @param start
   *                    - position of first result to retrieve
   * @param maxResult
   *                    - maximum number of results to return
   * @param hints
   *                    - Hibernate hint to set on the query
   * @return List of entities
   */
  public <E> List<E> resultListFromCriteria(CriteriaQuery<E> criteria, int start, int maxResult, Map<String, Object> hints) {
    TypedQuery<E> query = entityManager.createQuery(criteria);
    if (hints != null) {
      hints.forEach(query::setHint);
    }
    return query
            .setFirstResult(start)
            .setMaxResults(maxResult)
            .getResultList();
  }

  /**
   * Returns the resource count from a given predicate supplier.
   *
   * @param <E>               entity type
   * @param entityClass       - entity class to query cannot be null
   * @param predicateSupplier - function to return the predicates cannot be null but can return null
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

    Predicate[] predicates = predicateSupplier.supply(cb, root, entityManager);

    if (predicates != null) {
      countQuery.where(predicates);
    }
    return entityManager.createQuery(countQuery).getSingleResult();
  }
}
