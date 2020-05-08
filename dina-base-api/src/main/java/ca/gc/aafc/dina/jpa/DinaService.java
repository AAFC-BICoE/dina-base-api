package ca.gc.aafc.dina.jpa;

import java.util.List;
import java.util.Map;
import java.util.UUID;

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
    return null;// TODO
  }

  public E update(E entity) {
    return null;// TODO
  }

  public void delete(E entity) {
    // TODO
  }

  public List<E> findAllWhere(E entity, Map<String, Object> where) {
    return null;// TODO
  }

  public E findOne(UUID uuid) {
    return null;// TODO
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
