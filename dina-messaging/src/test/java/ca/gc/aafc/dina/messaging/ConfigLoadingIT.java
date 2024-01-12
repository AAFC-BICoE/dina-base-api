package ca.gc.aafc.dina.messaging;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import ca.gc.aafc.dina.messaging.config.RabbitMQProperties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import javax.inject.Inject;

@SpringBootTest
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class ConfigLoadingIT {

  @Inject
  private RabbitMQProperties rabbitMQProps;

  @Test
  public void validateConfig() {
    assertEquals("localhost", rabbitMQProps.getHost());
    assertNotEquals(0, rabbitMQProps.getPort());
  }

}
