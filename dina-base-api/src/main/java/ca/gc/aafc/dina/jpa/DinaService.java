package ca.gc.aafc.dina.jpa;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import ca.gc.aafc.dina.entity.DinaEntity;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class DinaService<E extends DinaEntity> {

  @NonNull
  private final BaseDAO baseDAO;

  public E create(E entity) {
    baseDAO.save(entity);
    return entity;
  }

  public E update(E entity) {
    return baseDAO.createWithEntityManager((em) -> {
      return em.merge(entity);
    });
  }

  public void delete(E entity) {
    baseDAO.delete(entity);
  }

  public List<E> findAllWhere(E entity, Map<String, Object> where) {
    return null;// TODO
  }

  public E findOne(Object naturalId, Class<E> entityClass) {
    return baseDAO.findOneByNaturalId(naturalId, entityClass);
  }

  public E preCreate(E entity) {
    return null;// TODO
  }

  public E preUpdate(E entity) {
    return null;// TODO
  }

  public void preDelete(E entity) {
    // TODO
  }
}
