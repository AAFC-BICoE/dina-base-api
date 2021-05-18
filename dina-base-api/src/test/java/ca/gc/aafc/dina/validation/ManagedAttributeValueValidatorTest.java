package ca.gc.aafc.dina.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
import java.util.AbstractMap;
import java.util.List;
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
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.entity.ManagedAttribute;
import ca.gc.aafc.dina.jpa.BaseDAO;
import ca.gc.aafc.dina.service.ManagedAttributeService;
import ca.gc.aafc.dina.validation.ManagedAttributeValueValidatorTest.ManagedAttributeConfig.TestManagedAttribute;
import ca.gc.aafc.dina.validation.ManagedAttributeValueValidatorTest.ManagedAttributeConfig.TestManagedAttributeService;
import groovy.transform.builder.Builder;
import lombok.Data;
import lombok.NonNull;

@Transactional
@SpringBootTest(classes = {TestDinaBaseApp.class, ManagedAttributeValueValidatorTest.ManagedAttributeConfig.class})
public class ManagedAttributeValueValidatorTest {

  @Inject
  private ManagedAttributeService<TestManagedAttribute> testManagedAttributeService;

  @Inject
  private ManagedAttributeValueValidator<TestManagedAttribute> validatorUnderTest;

  private TestManagedAttribute testManagedAttribute;

  @Test
  public void assignedValueContainedInAcceptedValues_validationPasses() throws Exception {
    testManagedAttribute = new TestManagedAttribute();
    testManagedAttribute.setName("key1");
    testManagedAttribute.setAcceptedValues(new String[] {"val1", "val2"});
    testManagedAttributeService.create(testManagedAttribute);

    Map.Entry<String, String> mav = new AbstractMap.SimpleEntry<>("key_1", "val1");

    Errors errors = new BeanPropertyBindingResult(mav, "mav");
    validatorUnderTest.validate(mav, errors);
    assertFalse(errors.hasFieldErrors());
    assertFalse(errors.hasErrors());
  }

  @Test
  public void assignedValueNotContainedInAcceptedValues_validationFails() throws Exception {
    testManagedAttribute = new TestManagedAttribute();
    testManagedAttribute.setName("key2");
    testManagedAttribute.setAcceptedValues(new String[] {"val1", "val2"});
    testManagedAttributeService.create(testManagedAttribute);

    Map.Entry<String, String> mav = new AbstractMap.SimpleEntry<>("key_2", "val3");

    Errors errors = new BeanPropertyBindingResult(mav, "mav");
    validatorUnderTest.validate(mav, errors);
    assertEquals(1, errors.getErrorCount());

    assertTrue(errors.getAllErrors().get(0).getCode().contains("val3"));
  }

  @Test
  public void assignedKeyDoesNotExist_validationFails() throws Exception {
    Map.Entry<String, String> mav = new AbstractMap.SimpleEntry<>("key_x", "val3");

    Errors errors = new BeanPropertyBindingResult(mav, "mav");
    assertThrows(IllegalArgumentException.class, () -> validatorUnderTest.validate(mav, errors)); 
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
    class TestManagedAttributeService extends ManagedAttributeService<TestManagedAttribute> {
      public TestManagedAttributeService(@NonNull BaseDAO baseDAO) {
        super(baseDAO);
      }
    }
  }
}
