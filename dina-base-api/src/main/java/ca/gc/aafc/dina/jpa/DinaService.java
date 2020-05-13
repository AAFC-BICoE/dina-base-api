package ca.gc.aafc.dina.jpa;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.stereotype.Component;

import ca.gc.aafc.dina.entity.DinaEntity;
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
  public List<E> findAllWhere(Class<E> entityClass, Map<String, Object> where) {
    Objects.requireNonNull(entityClass);
    Objects.requireNonNull(where);

    CriteriaBuilder criteriaBuilder = baseDAO.getCriteriaBuilder();
    CriteriaQuery<E> criteria = criteriaBuilder.createQuery(entityClass);
    Root<E> root = criteria.from(entityClass);

    Predicate[] predicates = where.entrySet().stream().map(entry -> {
      if (entry.getValue() == null) {
        return criteriaBuilder.isNull(root.get(entry.getKey()));
      }
      return criteriaBuilder.equal(root.get(entry.getKey()), entry.getValue());
    }).toArray(Predicate[]::new);

    criteria.where(predicates).select(root);

    return baseDAO.getResultList(criteria);
  }

  /**
   * Find an entity by it's NaturalId. The method assumes that the naturalId is
   * unique.
   * 
   * @param naturalId   - id of entity
   * @param entityClass - class of entity
   * @return the matched entity
   */
  public E findOne(Object naturalId, Class<E> entityClass) {
    return baseDAO.findOneByNaturalId(naturalId, entityClass);
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
