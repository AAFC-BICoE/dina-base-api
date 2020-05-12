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

@Component
@RequiredArgsConstructor(onConstructor_ = @Inject)
public abstract class DinaService<E extends DinaEntity> {

  @NonNull
  private final BaseDAO baseDAO;

  public E create(E entity) {
    preCreate(entity);
    baseDAO.save(entity);
    return entity;
  }

  public E update(E entity) {
    preUpdate(entity);
    return baseDAO.createWithEntityManager(em -> em.merge(entity));
  }

  public void delete(E entity) {
    baseDAO.delete(entity);
  }

  public List<E> findAllWhere(Class<E> entityClass, Map<String, Object> where) {
    Objects.requireNonNull(entityClass);
    Objects.requireNonNull(where);

    return baseDAO.createWithEntityManager(em -> {
      CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
      CriteriaQuery<E> criteria = criteriaBuilder.createQuery(entityClass);
      Root<E> root = criteria.from(entityClass);

      Predicate[] predicates = where.entrySet()
        .stream()
        .map(entry -> {
            if (entry.getValue() == null) {
              return criteriaBuilder.isNull(root.get(entry.getKey()));
            }
            return criteriaBuilder.equal(root.get(entry.getKey()), entry.getValue());
        })
        .toArray(Predicate[]::new);

      criteria.where(predicates).select(root);

      return em.createQuery(criteria).getResultList();
    });
  }

  public E findOne(Object naturalId, Class<E> entityClass) {
    return baseDAO.findOneByNaturalId(naturalId, entityClass);
  }

  public abstract E preCreate(E entity);

  public abstract E preUpdate(E entity);

  public abstract void preDelete(E entity);

}
