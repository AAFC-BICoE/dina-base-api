package ca.gc.aafc.dina.service;

import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.entity.ManagedAttribute;
import ca.gc.aafc.dina.jpa.BaseDAO;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Transactional
@SpringBootTest(classes = {TestDinaBaseApp.class, ManagedAttributeServiceIT.ManagedAttributeConfig.class})
public class ManagedAttributeServiceIT {

  @Inject
  private ManagedAttributeService<ManagedAttributeConfig.TestManagedAttribute> testManagedAttributeService;

  @Test
  public void managedAttributeService_OnCreate_KeyCorrectlyGenerated() {
    ManagedAttributeConfig.TestManagedAttribute managedAttribute = testManagedAttributeService
        .create(ManagedAttributeConfig.TestManagedAttribute.builder()
            .name("dina attribute #12").build());

    assertEquals("dina_attribute_12", managedAttribute.getKey());
  }

  @TestConfiguration
  @EntityScan(basePackageClasses = ManagedAttributeConfig.TestManagedAttribute.class)
  static class ManagedAttributeConfig {

    @Data
    @Builder
    @Entity
    static class TestManagedAttribute implements ManagedAttribute {
      @Id
      @GeneratedValue
      private Integer id;
      private UUID uuid;
      private String name;
      private String key;
      private ManagedAttributeType managedAttributeType;
      private String[] acceptedValues;
      private String createdBy;
      private OffsetDateTime createdOn;
    }

    @Service
    static class TestManagedAttributeService extends ManagedAttributeService<TestManagedAttribute> {
      public TestManagedAttributeService(@NonNull BaseDAO baseDAO) {
        super(baseDAO);
      }
    }
  }


}
