package ca.gc.aafc.dina.service;

import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.jpa.BaseDAO;
import ca.gc.aafc.dina.messaging.EntityChanged;
import ca.gc.aafc.dina.search.messaging.types.DocumentOperationType;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.validation.SmartValidator;

/**
 * Specialized {@link DefaultDinaService} that can emit events (using Spring {@link ApplicationEventPublisher})
 * when an entity changes.
 * @param <E>
 */
@Log4j2
public class MessageProducingService<E extends DinaEntity> extends DefaultDinaService<E> {

  private final String resourceType;
  private final ApplicationEventPublisher eventPublisher;

  public MessageProducingService(
    BaseDAO baseDAO,
    SmartValidator validator,
    String resourceType,
    ApplicationEventPublisher eventPublisher
  ) {
    super(baseDAO, validator);
    this.resourceType = resourceType;
    this.eventPublisher = eventPublisher;
  }

  @Override
  public E create(E entity) {
    E persisted = super.create(entity);
    publishEvent(persisted, DocumentOperationType.ADD);
    return persisted;
  }

  @Override
  public E update(E entity) {
    E persisted = super.update(entity);
    publishEvent(persisted, DocumentOperationType.UPDATE);
    return persisted;
  }

  @Override
  public void delete(E entity) {
    super.delete(entity);
    publishEvent(entity, DocumentOperationType.DELETE);
  }

  /**
   * Sends a message only after the main transaction is successful committed.
   * 
   * @param persisted entity to be sent as a message.
   * @param op operation type (example: DocumentOperationType.DELETE)
   */
  private void publishEvent(E persisted, DocumentOperationType op) {
    EntityChanged event = EntityChanged.builder().op(op)
        .resourceType(resourceType)
        .uuid(persisted.getUuid())
        .build();

    log.info("publishEvent: {}", event::toString);
    eventPublisher.publishEvent(event);
  }

}
