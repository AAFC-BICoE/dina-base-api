package ca.gc.aafc.dina.messaging.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import javax.inject.Inject;

/**
 * This test is making sure we can load 2 sets of properties for 2 different queues
 */
@SpringBootTest
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class ConfigLoadingIT {

  @Inject
  private RabbitMQProperties rabbitMQProps;

  @Inject
  private TestQueueProperties queueProperties;

  @Inject
  private TestSecondQueueProperties secondQueueProperties;

  @Test
  public void validateConfig() {
    assertEquals("localhost", rabbitMQProps.getHost());
    assertNotEquals(0, rabbitMQProps.getPort());

    assertEquals("testqueue1", queueProperties.getQueue());
    assertEquals("testqueue2", secondQueueProperties.getQueue());
  }

}
