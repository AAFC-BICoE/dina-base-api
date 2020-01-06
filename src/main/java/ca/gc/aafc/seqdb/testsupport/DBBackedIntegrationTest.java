package ca.gc.aafc.seqdb.testsupport;

import java.io.Serializable;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

/**
 * Provides database access for Integration tests.
 * All transactions are rollbacked at the end of a test.
 * Session is not exposed by design to ensure constant behaviors with transactions and caching.
 *
 */
public class DBBackedIntegrationTest {

  @Inject
  private EntityManager entityManager;
  
  public DBBackedIntegrationTest() {}
  
  public DBBackedIntegrationTest(EntityManager entityManager) {
    this.entityManager = entityManager;
  }
    
  /**
   * Save the provided object and the session and evict the object from the session. It will force a fresh load of the data
   * when find method will be called.
   * @param obj
   */
  protected void save(Object obj) {
    entityManager.persist(obj);
    entityManager.flush();
    entityManager.detach(obj);
  }
  
  protected <T> T find(Class<T> clazz, Serializable id) {
    T obj = entityManager.find(clazz, id);
    return obj;
  }
  
  /**
   * Find an entity based on the class, property and the property value.
   * 
   * @param clazz The entity class being retrieved.
   * @param property The property in the entity to query against. 
   * @param value The value of the property to find the entity against. 
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

}
