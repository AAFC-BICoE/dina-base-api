package ca.gc.aafc.dina.validation;

import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.entity.ManagedAttribute;
import ca.gc.aafc.dina.service.ManagedAttributeService;
import ca.gc.aafc.dina.service.ManagedAttributeServiceIT;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils;

import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
@SpringBootTest(classes = {TestDinaBaseApp.class, ManagedAttributeServiceIT.ManagedAttributeConfig.class})
public class ManagedAttributeValueValidatorTest {

  @Inject
  private ManagedAttributeService<ManagedAttributeServiceIT.TestManagedAttribute> testManagedAttributeService;

  @Inject
  private ManagedAttributeValueValidator<ManagedAttributeServiceIT.TestManagedAttribute> validatorUnderTest;

  private ManagedAttributeServiceIT.TestManagedAttribute testManagedAttribute;

  @Test
  void validate_WhenValidIntegerType() {
    testManagedAttribute = ManagedAttributeServiceIT.TestManagedAttribute.builder().
      name(RandomStringUtils.randomAlphabetic(6))
      .managedAttributeType(ManagedAttribute.ManagedAttributeType.INTEGER)
      .build();
    testManagedAttributeService.create(testManagedAttribute);

    Map<String, String> mav = Map.of(testManagedAttribute.getKey(), "1");

    Errors errors = new BeanPropertyBindingResult(mav, "mav");
    validatorUnderTest.validate(mav, errors);
    assertFalse(errors.hasFieldErrors());
    assertFalse(errors.hasErrors());
  }

  @Test
  public void assignedValueContainedInAcceptedValues_validationPasses() {
    testManagedAttribute = ManagedAttributeServiceIT.TestManagedAttribute.builder().
      name("My special Attribute").acceptedValues(new String[]{"val1", "val2"}).build();
    testManagedAttributeService.create(testManagedAttribute);

    Map<String, String> mav = Map.of(testManagedAttribute.getKey(), "val1");

    Errors errors = new BeanPropertyBindingResult(mav, "mav");
    validatorUnderTest.validate(mav, errors);
    assertFalse(errors.hasFieldErrors());
    assertFalse(errors.hasErrors());
  }

  @Test
  public void assignedValueNotContainedInAcceptedValues_validationFails() {
    testManagedAttribute = ManagedAttributeServiceIT.TestManagedAttribute.builder().
      name("key2").acceptedValues(new String[]{"val1", "val2"}).build();
    testManagedAttributeService.create(testManagedAttribute);

    Map<String, String> mav = Map.of(testManagedAttribute.getKey(), "val3");

    Errors errors = new BeanPropertyBindingResult(mav, "mav");
    validatorUnderTest.validate(mav, errors);
    assertEquals(1, errors.getErrorCount());

    assertTrue(errors.getAllErrors().get(0).getCode().contains("val3"));
  }

  @Test
  public void assignedKeyDoesNotExist_validationFails() {
    Map<String, String> mav = Map.of("key_x", "val3");

    Errors errors = new BeanPropertyBindingResult(mav, "mav");
    validatorUnderTest.validate(mav, errors);
    assertEquals(1, errors.getErrorCount());

    assertTrue(errors.getAllErrors().get(0).getCode().contains("assignedValue key not found."));
  }

  @Test
  public void validate_whenEmpty_NoErrors() {
    Map<String, String> mav = Map.of();
    Errors errors = new BeanPropertyBindingResult(mav, "mav");
    validatorUnderTest.validate(mav, errors);
    assertFalse(errors.hasFieldErrors());
    assertFalse(errors.hasErrors());
  }
}
