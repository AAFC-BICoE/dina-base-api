package ca.gc.aafc.dina.messaging.config;


import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;

/**
 * Base class for creating RabbitMQ consumer queues with Dead Letter Exchange (DLX) support.
 *
 * Messages that fail processing (mostly due to unhandled exceptions) are automatically routed to the configured Dead Letter Queue.
 * All queues use the default exchange ("") and queue name as routing key.
 *
 * This is a base class only - not a Spring configuration.
 * Concrete subclasses must:
 * <ul>
 *   <li>Add @Configuration annotation</li>
 *   <li>Add ConditionalOnProperty(prefix = "dina.messaging", name = "isConsumer", havingValue = "true")</li>
 *   <li>Add 2 methods createQueueBean() and createDeadLetterQueueBean() with @Bean annotations and explicit bean names</li>
 * </ul>
 */
public class RabbitMQConsumerConfiguration {

  private final String queueName;
  private final String deadLetterQueueName;

  public RabbitMQConsumerConfiguration(RabbitMQQueueProperties queueProperties) {
    queueName = queueProperties.getQueue();
    deadLetterQueueName = queueProperties.getDeadLetterQueue();
  }

  /**
   * Creates a Queue with a Dead Letter Exchange.
   * @return
   */
  public Queue createQueue() {
    return QueueBuilder.durable(queueName)
      .deadLetterExchange("")
      .deadLetterRoutingKey(deadLetterQueueName)
      .build();
  }

  public Queue createDeadLetterQueue() {
    return QueueBuilder.durable(deadLetterQueueName).build();
  }
}
