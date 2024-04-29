package ca.gc.aafc.dina.messaging.producer;

import ca.gc.aafc.dina.messaging.DinaMessage;

public interface MessageProducer {

  /**
   * Send a DinaMessage.
   */
  void send(DinaMessage dinaMessage);
}
