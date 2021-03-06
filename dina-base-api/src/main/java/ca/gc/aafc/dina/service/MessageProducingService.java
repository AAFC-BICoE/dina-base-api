package ca.gc.aafc.dina.service;

import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.jpa.BaseDAO;
import ca.gc.aafc.dina.search.messaging.producer.MessageProducer;
import ca.gc.aafc.dina.search.messaging.types.DocumentOperationNotification;
import ca.gc.aafc.dina.search.messaging.types.DocumentOperationType;
import org.springframework.validation.SmartValidator;

public class MessageProducingService<E extends DinaEntity> extends DefaultDinaService<E> {

  private final String resourceType;
  private final MessageProducer producer;

  public MessageProducingService(
    BaseDAO baseDAO,
    SmartValidator validator,
    String resourceType,
    MessageProducer messageProducer
  ) {
    super(baseDAO, validator);
    this.resourceType = resourceType;
    this.producer = messageProducer;
  }

  @Override
  public E create(E entity) {
    E persisted = super.create(entity);
    sendMessage(persisted, DocumentOperationType.ADD);
    return persisted;
  }

  @Override
  public E update(E entity) {
    E persisted = super.update(entity);
    sendMessage(persisted, DocumentOperationType.UPDATE);
    return persisted;
  }

  @Override
  public void delete(E entity) {
    super.delete(entity);
    sendMessage(entity, DocumentOperationType.DELETE);
  }

  private void sendMessage(E persisted, DocumentOperationType add) {
    producer.send(DocumentOperationNotification.builder()
      .operationType(add)
      .documentId(persisted.getUuid().toString())
      .documentType(resourceType)
      .build());
  }
}
