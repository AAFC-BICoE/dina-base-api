package ca.gc.aafc.dina.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
import java.util.AbstractMap;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.stereotype.Service;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.ValidationUtils;

import ca.gc.aafc.dina.DinaBaseApiAutoConfiguration;
import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.entity.ManagedAttribute;
import ca.gc.aafc.dina.entity.ManagedAttributeValue;
import ca.gc.aafc.dina.jpa.BaseDAO;
import ca.gc.aafc.dina.service.ManagedAttributeService;
import ca.gc.aafc.dina.validation.ManagedAttributeValueValidatorTest.ManagedAttributeConfig.TestManagedAttribute;
import groovy.transform.builder.Builder;
import lombok.Data;
import lombok.NonNull;

@Transactional
@SpringBootTest(classes = {TestDinaBaseApp.class, ManagedAttributeValueValidatorTest.ManagedAttributeConfig.class})
public class ManagedAttributeValueValidatorTest {

  @Inject
  private ManagedAttributeService<ManagedAttributeConfig.TestManagedAttribute> testManagedAttributeService;
  
  private TestManagedAttribute testManagedAttribute;
  private ReloadableResourceBundleMessageSource messageSource = messageSource();
  private ManagedAttributeValueValidator<TestManagedAttribute> validatorUnderTest; //= new ManagedAttributeValueValidator(messageSource, testManagedAttributeService);

  @Test
  public void assignedValueContainedInAcceptedValues_validationPasses() throws Exception {
    validatorUnderTest = new ManagedAttributeValueValidator<TestManagedAttribute>(messageSource, testManagedAttributeService);
    testManagedAttribute = new TestManagedAttribute();
    testManagedAttribute.setKey("key1");
    testManagedAttribute.setName("test_attribute");
    testManagedAttribute.setAcceptedValues(new String[] {"val1", "val2"});
    testManagedAttributeService.create(testManagedAttribute);

    Map.Entry<String, String> mav = new AbstractMap.SimpleEntry<>("key1", "val1");

    Errors errors = new BeanPropertyBindingResult(mav, "mav");
    ValidationUtils.invokeValidator(validatorUnderTest, mav, errors);
    assertFalse(errors.hasFieldErrors());
    assertFalse(errors.hasErrors());
  }

  @Test
  public void assignedValueNotContainedInAcceptedValues_validationFails() throws Exception {
    validatorUnderTest = new ManagedAttributeValueValidator(messageSource, testManagedAttributeService);

    testManagedAttribute = new TestManagedAttribute();
    testManagedAttribute.setName("test_attribute");
    testManagedAttribute.setKey("key2");
    testManagedAttribute.setAcceptedValues(new String[] {"val1", "val2"});

    Map.Entry<String, String> mav = new AbstractMap.SimpleEntry<>("key2", "val1");

    Errors errors = new BeanPropertyBindingResult(mav, "mav");
    ValidationUtils.invokeValidator(validatorUnderTest, mav, errors);
    assertEquals(1, errors.getErrorCount());
    assertTrue(errors.hasFieldErrors("assignedValue"));
    FieldError field_error = errors.getFieldError("assignedValue");
    assertTrue(field_error.getCode().equals("assignedValue.invalid"));
    assertTrue(field_error.getDefaultMessage().contains("val3"));
  }

  public static ReloadableResourceBundleMessageSource messageSource() {
    ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
    messageSource.setDefaultLocale(LocaleContextHolder.getLocale());
    messageSource.setBasename("classpath:validation-messages");
    messageSource.setDefaultEncoding("UTF-8");
    return messageSource;
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
