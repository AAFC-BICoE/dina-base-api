package ca.gc.aafc.dina.testsupport;

import java.io.Serializable;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

/**
 * Provides database access for Integration tests. All transactions are
 * rollbacked at the end of a test. Session is not exposed by design to ensure
 * constant behaviors with transactions and caching. *
 */
public class DBBackedIntegrationTest {

  @Inject
  private EntityManager entityManager;

  // Should only be used with runInNewTransaction
  @Inject
  private EntityManagerFactory entityManagerFactory;

  protected DBBackedIntegrationTest() {
  }

  public DBBackedIntegrationTest(EntityManagerFactory entityManagerFactory, EntityManager entityManager) {
    this.entityManagerFactory = entityManagerFactory;
    this.entityManager = entityManager;
  }

  /**
   * Save the provided object and the session and evict the object from the
   * session. It will force a fresh load of the data when find method will be
   * called.
   * 
   * @param obj
   */
  protected void save(Object obj) {
    save(obj, true);
  }

  protected void save(Object obj, boolean detach) {
    entityManager.persist(obj);
    entityManager.flush();
    if (detach) {
      entityManager.detach(obj);
    }
  }

  protected <T> T find(Class<T> clazz, Serializable id) {
    T obj = entityManager.find(clazz, id);
    return obj;
  }

  /**
   * See {@link EntityManager#detach(Object)}.
   * 
   * @param obj
   */
  protected void detach(Object obj) {
    entityManager.detach(obj);
  }

  /**
   * Find an entity based on the class, property and the property value.
   * 
   * @param clazz    The entity class being retrieved.
   * @param property The property in the entity to query against.
   * @param value    The value of the property to find the entity against.
   * @return The entity being retrieved.
   */
  protected <T> T findUnique(Class<T> clazz, String property, Object value) {
    // Create a criteria to retrieve the specific property.
    CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
    CriteriaQuery<T> criteria = criteriaBuilder.createQuery(clazz);
    Root<T> root = criteria.from(clazz);

    criteria.where(criteriaBuilder.equal(root.get(property), value));
    criteria.select(root);

    TypedQuery<T> query = entityManager.createQuery(criteria);

    return query.getSingleResult();
  }

  protected <T> void remove(Class<T> clazz, Serializable id) {
    entityManager.remove(entityManager.find(clazz, id));
    entityManager.flush();
  }

  /**
   * Accepts a {@link Consumer} of {@link EntityManager} that will be called in a
   * new, unmanaged transaction. The main goal is to not interfere with SpringTest
   * Managed transaction. Note that the Transaction will be committed.
   * 
   * This should only be used for setup/tear down purpose.
   * 
   * @param emConsumer
   */
  protected void runInNewTransaction(Consumer<EntityManager> emConsumer) {
    EntityManager em = entityManagerFactory.createEntityManager();
    EntityTransaction et = em.getTransaction();
    et.begin();
    emConsumer.accept(em);
    em.flush();
    et.commit();
    em.close();
  }

}
