package ca.gc.aafc.dina.messaging.producer;

import org.springframework.amqp.rabbit.core.RabbitTemplate;

import ca.gc.aafc.dina.messaging.DinaMessage;
import ca.gc.aafc.dina.messaging.config.RabbitMQQueueProperties;

/**
 * Generic class to send DinaMessage using default routing key (queue name).
 */
public class RabbitMQMessageProducer {

  private final RabbitTemplate rabbitTemplate;
  private final String queueName;

  public RabbitMQMessageProducer(RabbitTemplate rabbitTemplate,
                                 RabbitMQQueueProperties queueProperties) {
    this.rabbitTemplate = rabbitTemplate;
    this.queueName = queueProperties.getQueue();
  }

  public void send(DinaMessage dinaMessage) {
    // use the queue name as routing key since we are using the default
    rabbitTemplate.convertAndSend(queueName, dinaMessage);
  }

}
