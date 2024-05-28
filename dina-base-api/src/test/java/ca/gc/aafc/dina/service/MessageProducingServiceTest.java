package ca.gc.aafc.dina.service;

import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.entity.Item;
import ca.gc.aafc.dina.jpa.BaseDAO;
import ca.gc.aafc.dina.messaging.config.RabbitMQQueueProperties;
import ca.gc.aafc.dina.messaging.message.DocumentOperationNotification;
import ca.gc.aafc.dina.messaging.message.DocumentOperationType;
import ca.gc.aafc.dina.messaging.producer.DocumentOperationNotificationMessageProducer;
import ca.gc.aafc.dina.messaging.producer.DinaMessageProducer;
import ca.gc.aafc.dina.messaging.producer.RabbitMQMessageProducer;
import ca.gc.aafc.dina.testsupport.PostgresTestContainerInitializer;
import ca.gc.aafc.dina.testsupport.TransactionTestingHelper;

import javax.inject.Named;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.validation.SmartValidator;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

@SpringBootTest(classes = {TestDinaBaseApp.class, MessageProducingServiceTest.TestConfig.class},
  properties = {
    "dina.messaging.isProducer=true",
    "rabbitmq.queue=que",
    "rabbitmq.username=guest",
    "rabbitmq.password=guest",
    "rabbitmq.host=localhost"
  })
@ContextConfiguration(initializers = { PostgresTestContainerInitializer.class })
@DirtiesContext //it's an expensive test and we won't reuse the context
class MessageProducingServiceTest {

  @Inject
  private DefaultDinaService<Item> itemService;

  @Inject
  private RabbitTemplate template;

  @Inject
  private CachingConnectionFactory factory;

  @Inject
  private TestConfig.Listener listener;

  @Inject
  private TransactionTestingHelper transactionTestingHelper;

  public static final RabbitMQContainer CONTAINER = new RabbitMQContainer("rabbitmq:3-management-alpine");

  @BeforeAll
  static void beforeAll() {
    CONTAINER.withQueue("que");
    CONTAINER.withAdminPassword(null);
    CONTAINER.start();
  }

  @BeforeEach
  void setUp() {
    template.setConnectionFactory(factory);
    listener.getMessages().clear();
    listener.setLatch(new CountDownLatch(1));
  }

  @AfterAll
  static void afterAll() {
    CONTAINER.stop();
  }

  @SneakyThrows
  @Test
  void create() {
    Item item = Item.builder()
        .uuid(UUID.randomUUID())
        .group("CNC")
        .build();
    transactionTestingHelper.doInTransaction( ()-> itemService.create(item));
    listener.getLatch().await();

    assertResult(DocumentOperationType.ADD, item.getUuid().toString());
  }

  @SneakyThrows
  @Test
  void update() {
    Item item = Item.builder()
      .uuid(UUID.randomUUID())
      .group("CNC")
      .build();
    transactionTestingHelper.doInTransaction( ()-> itemService.create(item));
    listener.getLatch().await();
    listener.setLatch(new CountDownLatch(1));

    transactionTestingHelper.doInTransaction( ()-> itemService.update(item));
    listener.getLatch().await();

    assertResult(DocumentOperationType.UPDATE, item.getUuid().toString());
  }

  @SneakyThrows
  @Test
  void delete() {
    Item item = Item.builder()
      .uuid(UUID.randomUUID())
      .group("CNC")
      .build();
    transactionTestingHelper.doInTransaction( ()-> itemService.create(item));
    listener.getLatch().await();
    listener.setLatch(new CountDownLatch(1));

    // we need to load the entity before deleting it
    transactionTestingHelper.doInTransactionWithoutResult( (t) -> itemService.delete(itemService.findOne(item.getUuid(),
        Item.class)));
    listener.getLatch().await();

    assertResult(DocumentOperationType.DELETE, item.getUuid().toString());
  }

  private void assertResult(DocumentOperationType op, String id) throws java.io.IOException {
    DocumentOperationNotification result = mapResult(listener.getMessages().get(0));
    Assertions.assertFalse(result.isDryRun());
    Assertions.assertEquals(op, result.getOperationType());
    Assertions.assertEquals(id, result.getDocumentId());
    Assertions.assertEquals("item", result.getDocumentType());
  }

  private static DocumentOperationNotification mapResult(String content) throws java.io.IOException {
    return new ObjectMapper().readValue(content, DocumentOperationNotification.class);
  }

  @TestConfiguration
  @EntityScan(basePackageClasses = TestConfig.class)
  @EnableRabbit
  static class TestConfig {

    @Service
    public static class ItemService extends MessageProducingService<Item> {

      public ItemService(
        @NonNull BaseDAO baseDAO,
        @NonNull SmartValidator sv,
          ApplicationEventPublisher applicationEventPublisher
      ) {
        super(baseDAO, sv, "item", applicationEventPublisher);
      }

      @Override
      protected void preCreate(Item entity) {
        entity.setUuid(UUID.randomUUID());
      }
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(CachingConnectionFactory f) {
      f.setPort(CONTAINER.getMappedPort(5672));
      f.setHost(CONTAINER.getHost());
      SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
      factory.setConnectionFactory(f);
      factory.setConcurrentConsumers(3);
      factory.setMaxConcurrentConsumers(10);
      return factory;
    }

    @ConfigurationProperties(prefix = "rabbitmq")
    @Component
    @Named("searchQueueProperties")
    public static class SearchQueueProperties extends RabbitMQQueueProperties {
    }

    /**
     * RabbitMQ based message producer
     */
    @Service
    @ConditionalOnProperty(prefix = "dina.messaging", name = "isProducer", havingValue = "true")
    public static class SearchRabbitMQMessageProducer extends RabbitMQMessageProducer
      implements DinaMessageProducer, DocumentOperationNotificationMessageProducer {

      @Autowired
      public SearchRabbitMQMessageProducer(RabbitTemplate rabbitTemplate, @Named("searchQueueProperties")
      RabbitMQQueueProperties queueProperties) {
        super(rabbitTemplate, queueProperties);
      }

      @Override
      public void send(DocumentOperationNotification message) {
        super.send(message);
      }
    }

    @Component
    @Getter
    public static class Listener {
      @Setter
      private CountDownLatch latch = new CountDownLatch(1);
      private final List<String> messages = new ArrayList<>();

      @RabbitListener(bindings = @QueueBinding(
        value = @Queue(value = "que"),
        exchange = @Exchange(value = "que", ignoreDeclarationExceptions = "true")),
        containerFactory = "rabbitListenerContainerFactory"
      )
      public void processMessage(String data) {
        messages.clear();
        messages.add(data);
        latch.countDown();
      }

    }

  }
}
