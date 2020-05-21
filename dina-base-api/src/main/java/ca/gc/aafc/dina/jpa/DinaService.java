package ca.gc.aafc.dina.jpa;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.CriteriaBuilder.In;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.stereotype.Component;

import ca.gc.aafc.dina.entity.DinaEntity;
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
  public <T> List<T> findAllWhere(Class<T> entityClass, Map<String, Object> where) {
    return findAllWhere(entityClass, where, new HashMap<>());
  }

  /**
   * <p>
   * Returns a list of Entities of a given class where fields match a given
   * 'Where' map and fields are present in a given 'In' map.
   * <p>
   *
   * <p>
   * Given WHERE map maps field names to values. WHERE map can be empty to find
   * all. Where map can search on null values. WHERE map field names can be a dot ('.')
   * seperated path for filtering on nested values.
   * <p>
   * 
   * <p>
   * Given IN map maps field names to a List of Values the field value is contained in. 
   * IN map can be empty when not matching an In clause.
   * <p>
   *
   * @param <T>         - Type of entity
   * @param <O>         - Type of Object
   * @param entityClass - entity class to search on
   * @param where       - map of fieldName::Values to match on
   * @param in          - map of fieldName::List to match in
   * @return list of Entities
   */
  public <T, O> List<T> findAllWhere(Class<T> entityClass, Map<String, Object> where, Map<String, Collection<O>> in) {
    Objects.requireNonNull(entityClass);
    Objects.requireNonNull(where);
    Objects.requireNonNull(in);

    CriteriaBuilder criteriaBuilder = baseDAO.getCriteriaBuilder();
    CriteriaQuery<T> criteria = criteriaBuilder.createQuery(entityClass);
    Root<T> root = criteria.from(entityClass);

    Predicate[] wherePredicates = where.entrySet().stream()
      .map(entry -> generateWherePredicate(criteriaBuilder, root, entry.getValue(), entry.getKey()))
      .toArray(Predicate[]::new);

    Predicate[] inPredicates = in.entrySet().stream().map(entry -> {
      In<O> inClause = criteriaBuilder.in(root.get(entry.getKey()));
      entry.getValue().forEach(val -> inClause.value(val));
      return inClause;
    }).toArray(Predicate[]::new);

    criteria.where(ArrayUtils.addAll(wherePredicates, inPredicates)).select(root);

    return baseDAO.resultListFromCriteria(criteria);
  }

  /**
   * <p>
   * Generates a Predicate from a given root and criteria builder, where an entites attribute 
   * equals a given value. The field path is a dot ('.') seperated path to the root entities
   * attribute. The field path can handle nested paths using the dot notations.
   * <P>
   * 
   * <P>
   * Given value can be null to match where field is null.
   * <p>
   *
   * @param <T>             - Type of the root
   * @param criteriaBuilder - criteriaBuilder to generate predicate
   * @param root            - root of the original entity
   * @param value           - value the field path equals
   * @param fieldPath       - dot ('.') seperated path to the roots attribute.
   * @return Predicate matching the root entites attribute to a given value
   */
  private static <T> Predicate generateWherePredicate(
    CriteriaBuilder criteriaBuilder,
    Root<T> root,
    Object value,
    String fieldPath
  ) {
    String[] path = fieldPath.split(Pattern.quote("."));
    Path<Object> attributePath = root.get(path[0]);
    for (String pathString : Arrays.asList(path).subList(1, path.length)) {
      attributePath = attributePath.get(pathString);
    }
    if (value == null) {
      return criteriaBuilder.isNull(attributePath);
    }
    return criteriaBuilder.equal(attributePath, value);
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
