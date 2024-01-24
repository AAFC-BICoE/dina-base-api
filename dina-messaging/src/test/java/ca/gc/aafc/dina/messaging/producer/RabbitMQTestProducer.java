package ca.gc.aafc.dina.messaging.producer;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import ca.gc.aafc.dina.messaging.DinaTestMessage;
import ca.gc.aafc.dina.messaging.config.TestQueueProperties;

@Component
public class RabbitMQTestProducer extends RabbitMQMessageProducer<DinaTestMessage>{
  public RabbitMQTestProducer(RabbitTemplate rabbitTemplate,
                              TestQueueProperties queueProperties) {
    super(rabbitTemplate, queueProperties);
  }
}
