package ca.gc.aafc.dina.messaging;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.RabbitMQContainer;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
  properties = {
    "dina.messaging.isProducer=true",
    "dina.messaging.isConsumer=false"
  }
)
@Import(ErrorHandlingProducerIT.RabbitTemplateTestConfig.class)
public class ErrorHandlingProducerIT {

  public static final RabbitMQContainer rabbitMQContainer = new RabbitMQContainer("rabbitmq:3.8.20-management-alpine");

  @Inject
  private RabbitTemplateTestConfig rabbitTemplateTestConfig;

  @Inject
  private RabbitTemplate rabbitTemplate;

  @BeforeAll
  static void beforeAll() {
    rabbitMQContainer.start();
  }

  @AfterAll
  static void afterAll() {
    rabbitMQContainer.stop();
  }

  @DynamicPropertySource
  static void registerRabbitMQProperties(DynamicPropertyRegistry registry) {
    registry.add("rabbitmq.host", rabbitMQContainer::getHost);
    registry.add("rabbitmq.port", rabbitMQContainer::getAmqpPort);
    registry.add("rabbitmq.username", rabbitMQContainer::getAdminUsername);
    registry.add("rabbitmq.password", rabbitMQContainer::getAdminPassword);
  }

  @Test
  public void sendMessageToNonExistentQueue_receiverReceives() throws InterruptedException {
    DinaTestMessage myMessage = new DinaTestMessage("test-message Queue non-existing");
    rabbitTemplate.convertAndSend("non-existing", myMessage);

    assertTrue(rabbitTemplateTestConfig.getLatch().await(1000, TimeUnit.MILLISECONDS));
    assertNotNull(rabbitTemplateTestConfig.getReturnedMessage());
  }

  @TestConfiguration
  public static class RabbitTemplateTestConfig {

    private final CountDownLatch latch = new CountDownLatch(1);
    private ReturnedMessage returnedMessage;

    public CountDownLatch getLatch() {
      return latch;
    }

    public ReturnedMessage getReturnedMessage() {
      return returnedMessage;
    }

    @Bean
    public RabbitTemplate.ReturnsCallback returnCallback() {
      return m -> {
        returnedMessage = m;
        latch.countDown();
      };
    }
  }
}
