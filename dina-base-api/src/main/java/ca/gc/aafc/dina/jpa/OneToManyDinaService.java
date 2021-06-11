package ca.gc.aafc.dina.jpa;

import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.service.DefaultDinaService;
import lombok.NonNull;
import org.springframework.validation.SmartValidator;

import java.util.List;

public abstract class OneToManyDinaService<E extends DinaEntity> extends DefaultDinaService<E> {

  private final List<OneToManyFieldHandler<?, E>> handlers;

  public OneToManyDinaService(
    @NonNull BaseDAO baseDAO,
    @NonNull SmartValidator validator,
    @NonNull List<OneToManyFieldHandler<?, E>> handlers
  ) {
    super(baseDAO, validator);
    this.handlers = handlers;
  }

  @Override
  public E create(E entity) {
    handlers.forEach(h -> h.onCreate(entity));
    return super.create(entity);
  }

  @Override
  public E update(E entity) {
    handlers.forEach(h -> h.onUpdate(entity, this));
    return super.update(entity);
  }

  @Override
  public void delete(E entity) {
    handlers.forEach(h -> h.onDelete(entity));
    super.delete(entity);
  }

}
