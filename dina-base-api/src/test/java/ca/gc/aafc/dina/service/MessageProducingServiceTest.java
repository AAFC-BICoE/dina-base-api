package ca.gc.aafc.dina.service;

import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.dto.RelatedEntity;
import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.jpa.BaseDAO;
import ca.gc.aafc.dina.repository.meta.AttributeMetaInfoProvider;
import ca.gc.aafc.dina.search.messaging.producer.MessageProducer;
import io.crnk.core.resource.annotations.JsonApiId;
import io.crnk.core.resource.annotations.JsonApiResource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.hibernate.annotations.NaturalId;
import org.javers.core.metamodel.annotation.PropertyName;
import org.javers.core.metamodel.annotation.TypeName;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.stereotype.Service;
import org.springframework.validation.SmartValidator;

import javax.inject.Inject;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Transactional
@SpringBootTest(classes = {TestDinaBaseApp.class, MessageProducingServiceTest.TestConfig.class})
class MessageProducingServiceTest {

  @Inject
  private DefaultDinaService<TestConfig.Item> itemService;

  @Test
  void name() {
    Assertions.assertNotNull(itemService);
  }

  @TestConfiguration
  @EntityScan(basePackageClasses = TestConfig.class)
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

    @Data
    @EqualsAndHashCode(callSuper = true)
    @JsonApiResource(type = TestConfig.ItemDto.TYPE_NAME)
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @RelatedEntity(TestConfig.Item.class)
    @TypeName(TestConfig.ItemDto.TYPE_NAME)
    public static class ItemDto extends AttributeMetaInfoProvider {
      private static final String TYPE_NAME = "item";
      @JsonApiId
      @org.javers.core.metamodel.annotation.Id
      @PropertyName("id")
      private UUID uuid;
      private String group;
      private String createdBy;
      private OffsetDateTime createdOn;
    }

    @Service
    public static class ItemService extends MessageProducingService<TestConfig.Item> {

      public ItemService(
        @NonNull BaseDAO baseDAO,
        @NonNull SmartValidator sv,
        Optional<MessageProducer> producer
      ) {
        super(baseDAO, sv, producer, ItemDto.TYPE_NAME);
      }

      @Override
      protected void preCreate(TestConfig.Item entity) {
        entity.setUuid(UUID.randomUUID());
      }
    }

  }
}