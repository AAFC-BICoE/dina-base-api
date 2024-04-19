package ca.gc.aafc.dina.messaging.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Test properties for testqueue2
 */
@ConfigurationProperties(prefix = "rabbitmq.queue2")
@Component
public class TestSecondQueueProperties extends RabbitMQQueueProperties {
}
