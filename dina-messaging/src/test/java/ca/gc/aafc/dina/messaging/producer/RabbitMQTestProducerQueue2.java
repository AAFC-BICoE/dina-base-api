package ca.gc.aafc.dina.messaging.producer;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import ca.gc.aafc.dina.messaging.config.TestSecondQueueProperties;

@Component
public class RabbitMQTestProducerQueue2 extends RabbitMQMessageProducer {
  public RabbitMQTestProducerQueue2(RabbitTemplate rabbitTemplate,
                                    TestSecondQueueProperties queueProperties) {
    super(rabbitTemplate, queueProperties);
  }
}
