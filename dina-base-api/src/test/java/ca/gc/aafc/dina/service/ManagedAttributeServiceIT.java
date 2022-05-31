package ca.gc.aafc.dina.service;

import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.entity.ManagedAttribute;
import ca.gc.aafc.dina.i18n.MultilingualDescription;
import ca.gc.aafc.dina.jpa.BaseDAO;
import ca.gc.aafc.dina.validation.ManagedAttributeValueValidator;
import ca.gc.aafc.dina.validation.ValidationContext;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.validation.Errors;
import org.springframework.validation.SmartValidator;

import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.transaction.Transactional;
import javax.validation.constraints.NotNull;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Transactional
@SpringBootTest(classes = {TestDinaBaseApp.class, ManagedAttributeServiceIT.ManagedAttributeConfig.class})
public class ManagedAttributeServiceIT {

  @Inject
  private ManagedAttributeService<TestManagedAttribute> testManagedAttributeService;

  @Test
  public void managedAttributeService_OnCreate_KeyCorrectlyGenerated() {
    TestManagedAttribute managedAttribute = testManagedAttributeService
        .create(TestManagedAttribute.builder()
            .managedAttributeType(ManagedAttribute.ManagedAttributeType.STRING)
            .name("dina attribute #12").build());

    assertEquals("dina_attribute_12", managedAttribute.getKey());
  }

  @Test
  public void managedAttributeService_OnFindOne_OneReturned() {
    TestManagedAttribute managedAttribute1 = testManagedAttributeService
        .createAndFlush(TestManagedAttribute.builder()
            .managedAttributeType(ManagedAttribute.ManagedAttributeType.STRING)
            .name("dina attribute 1").build());
    testManagedAttributeService
        .createAndFlush(TestManagedAttribute.builder()
            .managedAttributeType(ManagedAttribute.ManagedAttributeType.STRING)
            .name("dina attribute 2").build());

    TestManagedAttribute managedAttribute = testManagedAttributeService.findOneByKey(managedAttribute1.getKey());

    assertNotNull(managedAttribute1.getId());
    assertEquals(managedAttribute1.getId(), managedAttribute.getId());
  }

  @Test
  public void managedAttributeService_OnFindOneAnd_OneReturned() {
    // test setup allows duplicated name/key
    // we create a duplicate and use the createdBy to distinct them
    TestManagedAttribute managedAttribute1 = testManagedAttributeService
        .createAndFlush(TestManagedAttribute.builder()
            .managedAttributeType(ManagedAttribute.ManagedAttributeType.STRING)
            .name("dina attribute")
            .createdBy("abc").build());
    testManagedAttributeService
        .createAndFlush(TestManagedAttribute.builder()
            .managedAttributeType(ManagedAttribute.ManagedAttributeType.STRING)
            .name("dina attribute")
            .createdBy("bcd").build());

    TestManagedAttribute managedAttribute = testManagedAttributeService
        .findOneByKeyAnd(managedAttribute1.getKey(), Pair.of("createdBy", "abc"));

    assertNotNull(managedAttribute1.getId());
    assertEquals(managedAttribute1.getId(), managedAttribute.getId());
  }

  @TestConfiguration
  @EntityScan(basePackageClasses = TestManagedAttribute.class)
  public static class ManagedAttributeConfig {

    @Bean
    public ManagedAttributeService<TestManagedAttribute> managedAttributeService(BaseDAO baseDAO, SmartValidator sv) {
      return new ManagedAttributeService<>(baseDAO , sv, TestManagedAttribute.class) {
      };
    }

    @Bean
    public ManagedAttributeValueValidator<TestManagedAttribute> managedAttributeValueValidator(
        @Named("validationMessageSource") MessageSource messageSource,
        @NonNull ManagedAttributeService<TestManagedAttribute> dinaService) {

      return new ManagedAttributeValueValidator<>(messageSource, dinaService) {
        @Override
        protected boolean preValidateValue(TestManagedAttribute managedAttributeDefinition,
            String value, Errors errors, ValidationContext vc) {
          if(managedAttributeDefinition.isFailValidateValue()) {
            errors.reject("failValidateValue is true");
            return false;
          }
          return true;
        }

        /**
         * Override this method to allow uniqueness by key/component (instead of only key).
         * @param keys
         * @param validationContext
         * @return
         */
        @Override
        protected Map<String, TestManagedAttribute> findAttributesForValidation(
            Set<String> keys, ValidationContext validationContext) {
          return dinaService.findAttributesForKeys(keys, Pair.of("component",
               validationContext.getValue()));
        }
      };
    }
  }

  /**
   * Test implementation of a {@link ManagedAttribute}.
   * Since it's running on H2 the uniqueness if not really define so the test will assume
   * it is by key or key/component depending on the purpose of the test.
   */
  @Data
  @Builder
  @Entity
  @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
  public static class TestManagedAttribute implements ManagedAttribute {
    @Id
    @GeneratedValue
    private Integer id;
    private UUID uuid;
    private String name;
    private String key;
    @NotNull
    private ManagedAttributeType managedAttributeType;
    private String[] acceptedValues;
    private String createdBy;
    private OffsetDateTime createdOn;
    //for testing purpose
    private boolean failValidateValue;
    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private MultilingualDescription multilingualDescription;

    // matches XYZValidationContext toString
    private String component;
  }

  @Data
  @Builder
  public static class TestManagedAttributeUsage implements DinaEntity {
    @Id
    @GeneratedValue
    private Integer id;
    private UUID uuid;

    private Map<String, String> managedAttributes;

    private String createdBy;
    private OffsetDateTime createdOn;
  }

  public enum XYZValidationContext implements ValidationContext {
    X, Y, Z;

    @Override
    public Object getValue() {
      return toString();
    }
  }

}
