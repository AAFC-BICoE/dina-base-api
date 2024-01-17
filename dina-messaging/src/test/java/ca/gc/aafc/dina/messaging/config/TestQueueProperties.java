package ca.gc.aafc.dina.messaging.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "rabbitmq")
@Component
public class TestQueueProperties extends RabbitMQQueueProperties {
}
