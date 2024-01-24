package ca.gc.aafc.dina.messaging.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Test properties for testqueue1
 */
@ConfigurationProperties(prefix = "rabbitmq.queue1")
@Component
public class TestQueueProperties extends RabbitMQQueueProperties {
}
