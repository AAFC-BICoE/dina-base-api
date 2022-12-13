package ca.gc.aafc.dina.service;

import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.entity.ma.TestManagedAttribute;
import ca.gc.aafc.dina.jpa.BaseDAO;
import ca.gc.aafc.dina.testsupport.PostgresTestContainerInitializer;
import ca.gc.aafc.dina.validation.ManagedAttributeValueValidator;
import ca.gc.aafc.dina.validation.ValidationContext;
import ca.gc.aafc.dina.vocabulary.TypedVocabularyElement;
import lombok.NonNull;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.validation.Errors;
import org.springframework.validation.SmartValidator;

import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.Transactional;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Transactional
@SpringBootTest(classes = {TestDinaBaseApp.class, ManagedAttributeServiceIT.ManagedAttributeConfig.class})
@ContextConfiguration(initializers = { PostgresTestContainerInitializer.class })
public class ManagedAttributeServiceIT {

  @Inject
  private ManagedAttributeService<TestManagedAttribute> testManagedAttributeService;

  @Test
  public void managedAttributeService_OnCreate_KeyCorrectlyGenerated() {
    TestManagedAttribute managedAttribute = testManagedAttributeService
        .create(TestManagedAttribute.builder()
            .uuid(UUID.randomUUID())
            .managedAttributeType(TypedVocabularyElement.VocabularyElementType.STRING)
            .name("dina attribute #12").build());

    assertEquals("dina_attribute_12", managedAttribute.getKey());
  }

  @Test
  public void managedAttributeService_OnFindOne_OneReturned() {
    TestManagedAttribute managedAttribute1 = testManagedAttributeService
        .createAndFlush(TestManagedAttribute.builder()
            .uuid(UUID.randomUUID())
            .managedAttributeType(TypedVocabularyElement.VocabularyElementType.STRING)
            .name("dina attribute 1").build());
    testManagedAttributeService
        .createAndFlush(TestManagedAttribute.builder()
            .uuid(UUID.randomUUID())
            .managedAttributeType(TypedVocabularyElement.VocabularyElementType.STRING)
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
            .managedAttributeType(TypedVocabularyElement.VocabularyElementType.STRING)
            .uuid(UUID.randomUUID())
            .name("dina attribute")
            .createdBy("abc").build());
    testManagedAttributeService
        .createAndFlush(TestManagedAttribute.builder()
            .managedAttributeType(TypedVocabularyElement.VocabularyElementType.STRING)
            .uuid(UUID.randomUUID())
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

  public enum XYZValidationContext implements ValidationContext {
    X, Y, Z;

    @Override
    public Object getValue() {
      return toString();
    }
  }

}
