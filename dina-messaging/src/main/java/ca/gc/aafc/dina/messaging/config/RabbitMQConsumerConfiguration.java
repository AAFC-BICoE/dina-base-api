package ca.gc.aafc.dina.messaging.config;


import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;

/**
 *
 * Creates a bean representing the queue.
 * A Dead Letter Queue (DLQ) is also created to store messages that can't be processed (mostly due to unhandled exceptions).
 *
 * All queues (including DLQ) are using the default exchange ("", empty string) and default routing key (name of the queue).
 *
 * This class is not declared as Configuration by default since the number of queues is unknown.
 * If more than 1 queue is required, override the create methods to set a name ont he beans otherwise only 1 will be created.
 *
 * Create a concrete class and add the 2 following annotations:
 * Configuration
 * ConditionalOnProperty(prefix = "dina.messaging", name = "isConsumer", havingValue = "true")
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
  @Bean
  public Queue createQueue() {
    return QueueBuilder.durable(queueName)
      .deadLetterExchange("")
      .deadLetterRoutingKey(deadLetterQueueName)
      .build();
  }

  @Bean
  public Queue createDeadLetterQueue() {
    return QueueBuilder.durable(deadLetterQueueName).build();
  }

}
