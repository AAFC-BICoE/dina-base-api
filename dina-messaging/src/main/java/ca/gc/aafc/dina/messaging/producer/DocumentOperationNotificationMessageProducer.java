package ca.gc.aafc.dina.messaging.producer;

import ca.gc.aafc.dina.messaging.message.DocumentOperationNotification;

/**
 * MessageProducer interface specific to {@link DocumentOperationNotification}.
 */
public interface DocumentOperationNotificationMessageProducer {

  /**
   * Send a DinaMessage.
   */
  void send(DocumentOperationNotification message);
}
