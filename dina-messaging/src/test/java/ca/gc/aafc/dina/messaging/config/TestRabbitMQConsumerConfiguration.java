package ca.gc.aafc.dina.messaging.config;

import org.springframework.amqp.core.Queue;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "dina.messaging", name = "isConsumer", havingValue = "true")
public class TestRabbitMQConsumerConfiguration extends RabbitMQConsumerConfiguration{
  public TestRabbitMQConsumerConfiguration(TestQueueProperties queueProperties) {
    super(queueProperties);
  }

  @Bean("queue1")
  public Queue createQueue1() {
    return createQueue();
  }

  @Bean("dlq1")
  public Queue createDeadLetterQueue1() {
    return createDeadLetterQueue();
  }
}
