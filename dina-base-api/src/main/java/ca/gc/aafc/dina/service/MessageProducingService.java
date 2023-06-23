package ca.gc.aafc.dina.service;

import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.jpa.BaseDAO;
import ca.gc.aafc.dina.messaging.EntityChanged;
import ca.gc.aafc.dina.messaging.MessageQueueNotifier;
import ca.gc.aafc.dina.search.messaging.types.DocumentOperationType;

import java.util.EnumSet;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.validation.SmartValidator;

/**
 * Specialized {@link DefaultDinaService} that can emit events (using Spring {@link ApplicationEventPublisher})
 * when an entity changes.
 * see {@link MessageQueueNotifier}
 * @param <E>
 */
@Log4j2
public class MessageProducingService<E extends DinaEntity> extends DefaultDinaService<E> {

  private final String resourceType;
  private final ApplicationEventPublisher eventPublisher;
  private final EnumSet<DocumentOperationType> supportedMessageOperations;

  public MessageProducingService(
    BaseDAO baseDAO,
    SmartValidator validator,
    String resourceType,
    ApplicationEventPublisher eventPublisher
  ) {
    this(baseDAO, validator, resourceType,
      EnumSet.of(DocumentOperationType.ADD, DocumentOperationType.UPDATE,
        DocumentOperationType.DELETE),
      eventPublisher);
  }

  /**
   *
   * @param baseDAO
   * @param validator
   * @param resourceType
   * @param supportedMessageOperations operations that should send a message
   * @param eventPublisher
   */
  public MessageProducingService(
    BaseDAO baseDAO,
    SmartValidator validator,
    String resourceType,
    EnumSet<DocumentOperationType> supportedMessageOperations,
    ApplicationEventPublisher eventPublisher
  ) {
    super(baseDAO, validator);
    this.resourceType = resourceType;
    this.eventPublisher = eventPublisher;
    this.supportedMessageOperations = supportedMessageOperations;
  }

  @Override
  public E create(E entity) {
    return create(entity, supportedMessageOperations.contains(DocumentOperationType.ADD));
  }

  /**
   * Create the given entity and emit a message if emitMessage is true.
   * @param entity
   * @param emitMessage
   * @return
   */
  public E create(E entity, boolean emitMessage) {
    E persisted = super.create(entity);
    if (emitMessage) {
      triggerEvent(persisted, DocumentOperationType.ADD);
    }
    return persisted;
  }

  @Override
  public E update(E entity) {
    return update(entity, supportedMessageOperations.contains(DocumentOperationType.UPDATE));
  }

  /**
   * Update the given entity and emit a message if emitMessage is true.
   * @param entity
   * @param emitMessage
   * @return
   */
  public E update(E entity, boolean emitMessage) {
    E persisted = super.update(entity);
    if (emitMessage) {
      triggerEvent(persisted, DocumentOperationType.UPDATE);
    }
    return persisted;
  }

  @Override
  public void delete(E entity) {
    delete(entity, supportedMessageOperations.contains(DocumentOperationType.DELETE));
  }

  /**
   * Delete the given entity and emit a message if emitMessage is true.
   * @param entity
   * @param emitMessage
   * @return
   */
  public void delete(E entity, boolean emitMessage) {
    super.delete(entity);
    if (emitMessage) {
      triggerEvent(entity, DocumentOperationType.DELETE);
    }
  }

  /**
   * Sends a message only after the main transaction is successfully committed.
   * 
   * @param persisted entity to be sent as a message.
   * @param op operation type (example: DocumentOperationType.DELETE)
   */
  protected void triggerEvent(E persisted, DocumentOperationType op) {
    EntityChanged event = EntityChanged.builder().op(op)
        .resourceType(resourceType)
        .uuid(persisted.getUuid())
        .build();

    publishEvent(event);
    postPublishEvent(persisted, op);
  }

  /**
   * Method used to publish an event.
   * The message will only be sent after the main transaction is successfully committed.
   *
   * @param event
   */
  protected void publishEvent(EntityChanged event) {
    log.info("publishEvent: {}", event::toString);
    eventPublisher.publishEvent(event);
  }

  /**
   * Override this method to be called after an event is published.
   * @param persisted
   * @param op
   */
  protected void postPublishEvent(E persisted, DocumentOperationType op) {
    // nothing by default
  }

}
