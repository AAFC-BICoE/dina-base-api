package ca.gc.aafc.dina.messaging.config;

import lombok.Getter;
import lombok.Setter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Loaded from application.yml
 */
@ConfigurationProperties(prefix = "rabbitmq")
@Component
@Getter
@Setter
@Validated
public class RabbitMQProperties {

  private String host;
  private String username;
  private String password;
  private int port;

}
