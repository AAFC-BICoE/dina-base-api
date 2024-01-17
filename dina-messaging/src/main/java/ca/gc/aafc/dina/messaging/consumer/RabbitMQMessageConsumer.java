package ca.gc.aafc.dina.messaging.consumer;

import ca.gc.aafc.dina.messaging.DinaMessage;

/**
 * Interface to receive message.
 *
 * Annotate the receiveMessage method with:
 *   RabbitListener(queues = "#{queueName}") where queueName is a variable if the class (from RabbitMQQueueProperties).
 *
 */
public interface RabbitMQMessageConsumer<M extends DinaMessage> {
  void receiveMessage(final M message);
}
