package ca.gc.aafc.auto;

import lombok.extern.log4j.Log4j2;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ca.gc.aafc.dina.messaging.MessageQueueNotifier;
import ca.gc.aafc.dina.messaging.producer.DocumentOperationNotificationMessageProducer;

/**
 * This class is outside the ComponentScan that is using DinaBaseApiAutoConfiguration base package.
 */
@Log4j2
@Configuration
public class MessageQueueNotifierAutoConfiguration {

  @ConditionalOnBean(DocumentOperationNotificationMessageProducer.class)
  @Bean
  public MessageQueueNotifier messageQueueNotifier(
    DocumentOperationNotificationMessageProducer messageProducer) {
    log.info("DocumentOperationNotificationMessageProducer available: MessageQueueNotifier created");
    return new MessageQueueNotifier(messageProducer);
  }

}
