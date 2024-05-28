package ca.gc.aafc.auto;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ca.gc.aafc.dina.messaging.MessageQueueNotifier;
import ca.gc.aafc.dina.messaging.producer.DocumentOperationNotificationMessageProducer;

/**
 * This class is outside the ComponentScan that is using DinaBaseApiAutoConfiguration base package.
 */
@Configuration
public class MessageQueueNotifierAutoConfiguration {

  @ConditionalOnBean(DocumentOperationNotificationMessageProducer.class)
  @Bean
  public MessageQueueNotifier messageQueueNotifier(
    DocumentOperationNotificationMessageProducer messageProducer) {
    return new MessageQueueNotifier(messageProducer);
  }

}
