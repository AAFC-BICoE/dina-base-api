package ca.gc.aafc.dina.messaging;

import java.util.concurrent.TimeUnit;
import javax.inject.Inject;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.RabbitMQContainer;

import ca.gc.aafc.dina.messaging.config.RabbitMQProperties;
import ca.gc.aafc.dina.messaging.consumer.RabbitMQTestConsumer;
import ca.gc.aafc.dina.messaging.consumer.RabbitMQTestConsumerQueue2;
import ca.gc.aafc.dina.messaging.producer.RabbitMQTestProducer;
import ca.gc.aafc.dina.messaging.producer.RabbitMQTestProducerQueue2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Complete test that demonstrate 2 producers and 2 consumers working on 2 queues.
 */
@SpringBootTest(
  properties = {
    "dina.messaging.isProducer=true",
    "dina.messaging.isConsumer=true"
  }
)
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class ConsumerProducerIT {

  public static final RabbitMQContainer rabbitMQContainer = new RabbitMQContainer("rabbitmq:3.8.20-management-alpine");

  @Inject
  private RabbitMQProperties rabbitMQProps;

  @Inject
  private RabbitMQTestProducer rabbitMQTestProducer;

  @Inject
  private RabbitMQTestConsumer rabbitMQTestConsumer;

  @Inject
  private RabbitMQTestProducerQueue2 rabbitMQTestProducerQueue2;

  @Inject
  private RabbitMQTestConsumerQueue2 rabbitMQTestConsumerQueue2;

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
  public void producer_onMessageSent_receiverReceives() throws InterruptedException {
    DinaTestMessage myMessage = new DinaTestMessage("test-message Queue 1");
    DinaTestMessage myMessageQueue2 = new DinaTestMessage("test-message Queue 2");

    rabbitMQTestProducer.send(myMessage);
    rabbitMQTestProducerQueue2.send(myMessageQueue2);

    // give 10 seconds to the latch to get to 0
    assertTrue(rabbitMQTestConsumer.getLatch().await(10, TimeUnit.SECONDS));
    assertEquals(myMessage.getName(), rabbitMQTestConsumer.getMessageReceived().getName());

    assertTrue(rabbitMQTestConsumerQueue2.getLatch().await(10, TimeUnit.SECONDS));
    assertEquals(myMessageQueue2.getName(), rabbitMQTestConsumerQueue2.getMessageReceived().getName());

  }

}
