package ca.gc.aafc.dina.messaging.producer;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import ca.gc.aafc.dina.messaging.DinaTestMessage;
import ca.gc.aafc.dina.messaging.config.TestQueueProperties;
import ca.gc.aafc.dina.messaging.config.TestSecondQueueProperties;

@Component
public class RabbitMQTestProducerQueue2 extends RabbitMQMessageProducer<DinaTestMessage>{
  public RabbitMQTestProducerQueue2(RabbitTemplate rabbitTemplate,
                                    TestSecondQueueProperties queueProperties) {
    super(rabbitTemplate, queueProperties);
  }
}
