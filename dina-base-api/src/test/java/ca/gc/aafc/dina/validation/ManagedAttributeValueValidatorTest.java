package ca.gc.aafc.dina.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
import java.util.AbstractMap;
import java.util.Map;
import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.ValidationUtils;

import ca.gc.aafc.dina.DinaBaseApiAutoConfiguration;
import ca.gc.aafc.dina.entity.ManagedAttribute;
import ca.gc.aafc.dina.entity.ManagedAttributeValue;
import groovy.transform.builder.Builder;
import lombok.Data;

public class ManagedAttributeValueValidatorTest {
  
  private TestManagedAttribute testManagedAttribute;
  private TestManagedAttributeValue testManagedAttributeValue;
  private static final ReloadableResourceBundleMessageSource messageSource = messageSource();
  private static final ManagedAttributeValueValidator validatorUnderTest = new ManagedAttributeValueValidator(messageSource);

  @Test
  public void assignedValueContainedInAcceptedValues_validationPasses() throws Exception {
    testManagedAttribute = new TestManagedAttribute();
    testManagedAttribute.setName("test_attribute");
    testManagedAttribute.setAcceptedValues(new String[] {"val1", "val2"});

    testManagedAttributeValue = new TestManagedAttributeValue();
    testManagedAttributeValue.setManagedAttribute(testManagedAttribute);
    testManagedAttributeValue.setMetadata(new AbstractMap.SimpleEntry<>("assignedValue", "val1"));

    Errors errors = new BeanPropertyBindingResult(testManagedAttributeValue, "mav");
    ValidationUtils.invokeValidator(validatorUnderTest, testManagedAttributeValue, errors);
    assertFalse(errors.hasFieldErrors());
    assertFalse(errors.hasErrors());
  }

  @Test
  public void assignedValueNotContainedInAcceptedValues_validationFails() throws Exception {
    testManagedAttribute = new TestManagedAttribute();
    testManagedAttribute.setName("test_attribute");
    testManagedAttribute.setAcceptedValues(new String[] {"val1", "val2"});

    testManagedAttributeValue = new TestManagedAttributeValue();
    testManagedAttributeValue.setManagedAttribute(testManagedAttribute);
    testManagedAttributeValue.setMetadata(new AbstractMap.SimpleEntry<>("assignedValue", "val3"));

    Errors errors = new BeanPropertyBindingResult(testManagedAttributeValue, "mav");
    ValidationUtils.invokeValidator(validatorUnderTest, testManagedAttributeValue, errors);
    assertEquals(1, errors.getErrorCount());
    assertTrue(errors.hasFieldErrors("assignedValue"));
    FieldError field_error = errors.getFieldError("assignedValue");
    assertTrue(field_error.getCode().equals("assignedValue.invalid"));
    assertTrue(field_error.getDefaultMessage().contains("val3"));
  }
  
  @Data
  @Builder
  @Entity
  public class TestManagedAttribute implements ManagedAttribute {
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
  
  @Data
  @Builder
  @Entity
  public class TestManagedAttributeValue implements ManagedAttributeValue {
    @Id
    @GeneratedValue
    private Integer id;
    private UUID uuid;
    private String createdBy;
    private OffsetDateTime createdOn;
    private Map.Entry<String, String> metadata;
    private TestManagedAttribute managedAttribute;
  }

  public static ReloadableResourceBundleMessageSource messageSource() {
    ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
    messageSource.setDefaultLocale(LocaleContextHolder.getLocale());
    messageSource.setBasename("classpath:validation-messages");
    messageSource.setDefaultEncoding("UTF-8");
    return messageSource;
  }
}
