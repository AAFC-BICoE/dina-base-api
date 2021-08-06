package ca.gc.aafc.dina.service;

import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.jpa.BaseDAO;
import ca.gc.aafc.dina.search.messaging.producer.MessageProducer;
import ca.gc.aafc.dina.search.messaging.types.DocumentOperationNotification;
import ca.gc.aafc.dina.search.messaging.types.DocumentOperationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;
import org.hibernate.annotations.NaturalId;
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
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.validation.SmartValidator;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import javax.inject.Inject;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

@Transactional
@SpringBootTest(classes = {TestDinaBaseApp.class, MessageProducingServiceTest.TestConfig.class},
  properties = {
    "messaging.isProducer=true",
    "rabbitmq.queue=que",
    "rabbitmq.exchange=exchange",
    "rabbitmq.routingkey=routingkey",
    "rabbitmq.username=guest",
    "rabbitmq.password=guest",
    "rabbitmq.host=localhost",
    "rabbitmq.port=49198"
  })
class MessageProducingServiceTest {

  @Inject
  private DefaultDinaService<TestConfig.Item> itemService;

  @Inject
  private RabbitTemplate template;

  @Inject
  private CachingConnectionFactory factory;

  @Inject
  private TestConfig.Listener listener;

  public static final RabbitMQContainer CONTAINER = new RabbitMQContainer("rabbitmq:3-management-alpine");

  @BeforeAll
  static void beforeAll() {
    CONTAINER.withQueue("que");
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
    TestConfig.Item item = TestConfig.Item.builder()
      .uuid(UUID.randomUUID())
      .group("CNC")
      .build();
    itemService.create(item);
    listener.getLatch().await();

    assertResult(DocumentOperationType.ADD, item.getUuid().toString());
  }

  @SneakyThrows
  @Test
  void update() {
    TestConfig.Item item = TestConfig.Item.builder()
      .uuid(UUID.randomUUID())
      .group("CNC")
      .build();
    itemService.create(item);
    listener.getLatch().await();
    listener.setLatch(new CountDownLatch(1));

    itemService.update(item);
    listener.getLatch().await();

    assertResult(DocumentOperationType.UPDATE, item.getUuid().toString());
  }

  @SneakyThrows
  @Test
  void delete() {
    TestConfig.Item item = TestConfig.Item.builder()
      .uuid(UUID.randomUUID())
      .group("CNC")
      .build();
    itemService.create(item);
    listener.getLatch().await();
    listener.setLatch(new CountDownLatch(1));

    itemService.delete(item);
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

    @Data
    @Entity
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item implements DinaEntity {
      private String createdBy;
      private OffsetDateTime createdOn;
      @Column(name = "group_name")
      private String group;
      @Id
      @GeneratedValue
      private Integer id;
      @NaturalId
      private UUID uuid;
    }

    @Service
    public static class ItemService extends MessageProducingService<TestConfig.Item> {

      public ItemService(
        @NonNull BaseDAO baseDAO,
        @NonNull SmartValidator sv,
        MessageProducer producer
      ) {
        super(baseDAO, sv, "item", producer);
      }

      @Override
      protected void preCreate(TestConfig.Item entity) {
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

    @Component
    @Getter
    public static class Listener {
      @Setter
      private CountDownLatch latch = new CountDownLatch(1);
      private final List<String> messages = new ArrayList<>();

      @RabbitListener(bindings = @QueueBinding(
        value = @Queue(value = "que"),
        exchange = @Exchange(value = "exchange", ignoreDeclarationExceptions = "true"),
        key = "routingkey"),
        containerFactory = "rabbitListenerContainerFactory"
      )
      public void processOrder(String data) {
        messages.clear();
        messages.add(data);
        latch.countDown();
      }

    }

  }
}
