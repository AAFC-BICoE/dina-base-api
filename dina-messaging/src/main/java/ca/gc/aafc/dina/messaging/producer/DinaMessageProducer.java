package ca.gc.aafc.dina.messaging.producer;

import ca.gc.aafc.dina.messaging.DinaMessage;

/**
 * Generic {@link DinaMessage} producer.
 */
public interface DinaMessageProducer {

  /**
   * Send a DinaMessage.
   */
  void send(DinaMessage dinaMessage);
}
