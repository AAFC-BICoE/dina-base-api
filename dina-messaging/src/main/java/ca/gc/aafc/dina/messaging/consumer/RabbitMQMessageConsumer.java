package ca.gc.aafc.dina.messaging.consumer;

import ca.gc.aafc.dina.messaging.DinaMessage;

/**
 * Interface to receive message.
 *
 * It is preferable to use SpEL to set the queue, the root will be BeanExpressionContext so a Bean must be used.
 *
 */
public interface RabbitMQMessageConsumer<M extends DinaMessage> {

  /**
   * Annotate implementation method with:
   * RabbitListener(queues = "#{searchQueueProperties.getQueue()}") where searchQueueProperties is the
   * name of the Bean of type RabbitMQQueueProperties
   * @param message the received message.
   */
  void receiveMessage(M message);
}
