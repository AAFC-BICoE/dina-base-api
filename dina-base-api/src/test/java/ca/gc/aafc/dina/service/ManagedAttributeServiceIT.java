package ca.gc.aafc.dina.service;

import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.entity.ManagedAttribute;
import ca.gc.aafc.dina.jpa.BaseDAO;
import ca.gc.aafc.dina.validation.ManagedAttributeValueValidator;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;
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
  private ManagedAttributeService<TestManagedAttribute> testManagedAttributeService;

  @Test
  public void managedAttributeService_OnCreate_KeyCorrectlyGenerated() {
    TestManagedAttribute managedAttribute = testManagedAttributeService
        .create(TestManagedAttribute.builder()
            .name("dina attribute #12").build());

    assertEquals("dina_attribute_12", managedAttribute.getKey());
  }

  @TestConfiguration
  @EntityScan(basePackageClasses = TestManagedAttribute.class)
  public static class ManagedAttributeConfig {

    @Bean
    public ManagedAttributeService<TestManagedAttribute> managedAttributeService(
        BaseDAO baseDAO) {
      return new ManagedAttributeService<>(baseDAO) {
      };
    }

    @Bean
    public ManagedAttributeValueValidator<TestManagedAttribute> managedAttributeValueValidator(
        @Named("validationMessageSource") MessageSource messageSource,
        @NonNull ManagedAttributeService<TestManagedAttribute> dinaService) {
      return new ManagedAttributeValueValidator<>(messageSource, dinaService);
    }
  }

  @Data
  @Builder
  @Entity
  public static class TestManagedAttribute implements ManagedAttribute {
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


}
