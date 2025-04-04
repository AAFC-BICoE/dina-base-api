package ca.gc.aafc.dina.messaging;

import org.springframework.context.event.EventListener;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import ca.gc.aafc.dina.messaging.message.DocumentOperationNotification;
import ca.gc.aafc.dina.messaging.producer.DocumentOperationNotificationMessageProducer;
import ca.gc.aafc.dina.messaging.producer.DinaMessageProducer;

import lombok.extern.log4j.Log4j2;

/**
 * This class will listen for {@link EntityChanged} event. The event will be processed AFTER
 * the commit of the transaction.
 * Then, it will emit a message using the provided {@link DinaMessageProducer}.
 */
@Log4j2
public class MessageQueueNotifier {

  private final DocumentOperationNotificationMessageProducer messageProducer;

  public MessageQueueNotifier(DocumentOperationNotificationMessageProducer messageProducer) {
    this.messageProducer = messageProducer;
  }

  // We are using fallbackExecution = true since we want it to proceed even if no transaction is
  // available
  @EventListener(EntityChanged.class)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void onEntityChanged(EntityChanged entityChanged) {
    log.info("Got entityChanged event: {}", entityChanged::toString);
    messageProducer.send(DocumentOperationNotification.builder()
        .operationType(entityChanged.getOp())
        .documentId(entityChanged.getUuid().toString())
        .documentType(entityChanged.getResourceType())
        .build());
  }

}
