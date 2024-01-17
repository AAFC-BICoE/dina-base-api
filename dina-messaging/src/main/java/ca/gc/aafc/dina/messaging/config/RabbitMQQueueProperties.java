package ca.gc.aafc.dina.messaging.config;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.lang3.StringUtils;

/**
 * Represents the configuration for a single queue.
 *
 * Use with the following annotations:
 * ConfigurationProperties(prefix = "rabbitmq")
 * Component
 * Named if more thant 1 queue is required.
 */
@Getter
@Setter
public class RabbitMQQueueProperties {
  static final String DEAD_LETTER_QUEUE_EXT = ".dlq";

  private String queue;
  private String deadLetterQueue;

  public String getDeadLetterQueue() {
    if (StringUtils.isBlank(deadLetterQueue)) {
      return queue + DEAD_LETTER_QUEUE_EXT;
    }
    return deadLetterQueue;
  }
}
