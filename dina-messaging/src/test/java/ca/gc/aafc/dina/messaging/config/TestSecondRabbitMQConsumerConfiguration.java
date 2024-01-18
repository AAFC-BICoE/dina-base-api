package ca.gc.aafc.dina.messaging.config;

import org.springframework.amqp.core.Queue;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "dina.messaging", name = "isConsumer", havingValue = "true")
public class TestSecondRabbitMQConsumerConfiguration extends RabbitMQConsumerConfiguration{
  public TestSecondRabbitMQConsumerConfiguration(TestSecondQueueProperties queueProperties) {
    super(queueProperties);
  }

  @Bean("queue2")
  @Override
  public Queue createQueue() {
    return super.createQueue();
  }

  @Bean("dlq")
  @Override
  public Queue createDeadLetterQueue() {
    return super.createDeadLetterQueue();
  }
}
