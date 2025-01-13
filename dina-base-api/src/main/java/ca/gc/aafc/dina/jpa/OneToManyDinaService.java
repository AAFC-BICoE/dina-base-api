package ca.gc.aafc.dina.jpa;

import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.service.DefaultDinaService;
import lombok.NonNull;
import org.springframework.validation.SmartValidator;

import java.util.List;

/**
 * @deprecated not used
 * OneToManyDinaService is a DefaultDinaService that will handle resolving associations for children of a
 * declared parent type. The constructor will require you to submit a list of {@link OneToManyFieldHandler},
 * one for each One to many field you want to resolve. Fields without handlers will not be resolved.
 *
 * @param <E> Type of Parent
 */
@Deprecated
public abstract class OneToManyDinaService<E extends DinaEntity> extends DefaultDinaService<E> {

  private final List<OneToManyFieldHandler<E, ?>> handlers;

  public OneToManyDinaService(
    BaseDAO baseDAO,
    SmartValidator validator,
    @NonNull List<OneToManyFieldHandler<E, ?>> handlers
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
