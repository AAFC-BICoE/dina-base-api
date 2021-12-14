package ca.gc.aafc.dina.validation;

import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.entity.Department;
import ca.gc.aafc.dina.entity.ManagedAttribute;
import ca.gc.aafc.dina.service.ManagedAttributeService;
import ca.gc.aafc.dina.service.ManagedAttributeServiceIT;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.ValidationException;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
@SpringBootTest(classes = {TestDinaBaseApp.class, ManagedAttributeServiceIT.ManagedAttributeConfig.class})
public class ManagedAttributeValueValidatorTest {

  private static final ManagedAttributeServiceIT.TestManagedAttributeUsage ENTITY_PLACEHOLDER = ManagedAttributeServiceIT.TestManagedAttributeUsage
      .builder()
      .uuid(UUID.randomUUID())
      .build();

  @Inject
  private ManagedAttributeService<ManagedAttributeServiceIT.TestManagedAttribute> testManagedAttributeService;

  @Inject
  private ManagedAttributeValueValidator<ManagedAttributeServiceIT.TestManagedAttribute> validatorUnderTest;

  private ManagedAttributeServiceIT.TestManagedAttribute testManagedAttribute;

  @Test
  void validate_WhenValidDateType_NoExceptionThrown() {
    testManagedAttribute = testManagedAttributeService.create(ManagedAttributeServiceIT.TestManagedAttribute.builder().
      name(RandomStringUtils.randomAlphabetic(6))
      .managedAttributeType(ManagedAttribute.ManagedAttributeType.DATE)
      .build());

    Map<String, String> mav = Map.of(testManagedAttribute.getKey(), LocalDate.now().toString());
    validatorUnderTest.validate(ENTITY_PLACEHOLDER, mav, ManagedAttributeServiceIT.XYZValidationContext.X);
  }

  @ParameterizedTest
  @ValueSource(strings = {"1.2", "", "  ", "\t", "\n", "a", "99-02-01", "11-23-2020", "11-1999-01", "01-01-01"})
  void validate_WhenInvalidDateType_ExceptionThrown(String value) {
    testManagedAttribute = testManagedAttributeService.create(ManagedAttributeServiceIT.TestManagedAttribute.builder().
      name(RandomStringUtils.randomAlphabetic(6))
      .managedAttributeType(ManagedAttribute.ManagedAttributeType.DATE)
      .build());

    Map<String, String> mav = Map.of(testManagedAttribute.getKey(), value);
    assertThrows(ValidationException.class, () -> validatorUnderTest.validate(ENTITY_PLACEHOLDER, mav));
  }

  @Test
  void validate_WhenValidStringType_NoExceptionThrown() {
    testManagedAttribute = ManagedAttributeServiceIT.TestManagedAttribute.builder().
      name(RandomStringUtils.randomAlphabetic(6))
      .managedAttributeType(ManagedAttribute.ManagedAttributeType.STRING)
      .build();
    testManagedAttributeService.create(testManagedAttribute);

    Map<String, String> mav = Map.of(testManagedAttribute.getKey(), "new string value");
    validatorUnderTest.validate(ENTITY_PLACEHOLDER, mav, ManagedAttributeServiceIT.XYZValidationContext.X);
  }

  @Test
  void validate_WhenValidIntegerType_NoExceptionThrown() {
    testManagedAttribute = ManagedAttributeServiceIT.TestManagedAttribute.builder().
      name(RandomStringUtils.randomAlphabetic(6))
      .managedAttributeType(ManagedAttribute.ManagedAttributeType.INTEGER)
      .build();
    testManagedAttributeService.create(testManagedAttribute);

    Map<String, String> mav = Map.of(testManagedAttribute.getKey(), "1");
    validatorUnderTest.validate(ENTITY_PLACEHOLDER, mav);
  }

  @ParameterizedTest
  @ValueSource(strings = {"1.2", "", "  ", "\t", "\n", "a"})
  void validate_WhenInvalidIntegerType_ExceptionThrown(String value) {
    testManagedAttribute = ManagedAttributeServiceIT.TestManagedAttribute.builder().
      name(RandomStringUtils.randomAlphabetic(6))
      .managedAttributeType(ManagedAttribute.ManagedAttributeType.INTEGER)
      .build();
    testManagedAttributeService.create(testManagedAttribute);

    Map<String, String> mav = Map.of(testManagedAttribute.getKey(), value);
    assertThrows(ValidationException.class, () -> validatorUnderTest.validate(ENTITY_PLACEHOLDER, mav));
  }

  @Test
  void validate_WhenInvalidIntegerTypeExceptionThrown() {
    testManagedAttribute = ManagedAttributeServiceIT.TestManagedAttribute.builder().
        name(RandomStringUtils.randomAlphabetic(6))
        .managedAttributeType(ManagedAttribute.ManagedAttributeType.INTEGER)
        .build();
    testManagedAttributeService.create(testManagedAttribute);
    Map<String, String> mav = Map.of(testManagedAttribute.getKey(), "1.2");
    assertThrows(ValidationException.class,
        () -> validatorUnderTest.validate(Department.builder().uuid(UUID.randomUUID()).build(), mav));
  }

  @Test
  void validate_WhenPreValidationFailsExceptionThrown() {
    testManagedAttribute = ManagedAttributeServiceIT.TestManagedAttribute.builder().
        name(RandomStringUtils.randomAlphabetic(6))
        .managedAttributeType(ManagedAttribute.ManagedAttributeType.INTEGER)
        .failValidateValue(true)
        .build();
    testManagedAttributeService.create(testManagedAttribute);
    Map<String, String> mav = Map.of(testManagedAttribute.getKey(), "2");
    assertThrows(ValidationException.class,
        () -> validatorUnderTest.validate(ENTITY_PLACEHOLDER, mav));
  }

  @Test
  public void assignedValueContainedInAcceptedValues_validationPasses() {
    testManagedAttribute = ManagedAttributeServiceIT.TestManagedAttribute.builder().
      name("My special Attribute").acceptedValues(new String[]{"val1", "val2"}).build();
    testManagedAttributeService.create(testManagedAttribute);

    Map<String, String> mav = Map.of(testManagedAttribute.getKey(), "val1");

    Errors errors = new BeanPropertyBindingResult(mav, "mav");
    validatorUnderTest.validate(mav, errors);
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

    assertTrue(errors.getAllErrors().get(0).getDefaultMessage().contains("val3"));
  }

  @Test
  public void assignedKeyDoesNotExist_ValidationExceptionThrown() {
    Map<String, String> mav = Map.of("key_x", "val3");
    assertThrows(ValidationException.class, () -> validatorUnderTest.validate(ENTITY_PLACEHOLDER, mav));
  }

  @Test
  public void validate_whenEmpty_NoErrors() {
    Map<String, String> mav = Map.of();
    Errors errors = new BeanPropertyBindingResult(mav, "mav");
    validatorUnderTest.validate(mav, errors);
    assertFalse(errors.hasErrors());
  }

  @Test
  void validate_IllegalParameters() {
    String wrongType = "wrong type";
    Map<String, Integer> wrongValueType = Map.of("string", 2);
    Map<Integer, String> wrongKeyType = Map.of(2, "");
    assertThrows(
      IllegalArgumentException.class,
      () -> validatorUnderTest.validate(wrongType, new BeanPropertyBindingResult(wrongType, "mav")));
    assertThrows(
      IllegalArgumentException.class,
      () -> validatorUnderTest.validate(
        wrongValueType,
        new BeanPropertyBindingResult(wrongValueType, "mav")));
    assertThrows(
      IllegalArgumentException.class,
      () -> validatorUnderTest.validate(wrongKeyType, new BeanPropertyBindingResult(wrongKeyType, "mav")));
  }

  @Test
  void validate_NonDinaEntity_NoErrors() {
    testManagedAttribute = testManagedAttributeService.create(ManagedAttributeServiceIT.TestManagedAttribute.builder()
    .name(RandomStringUtils.randomAlphabetic(6))
    .managedAttributeType(ManagedAttribute.ManagedAttributeType.DATE)
    .build());

  Map<String, String> mav = Map.of(testManagedAttribute.getKey(), LocalDate.now().toString());
  
  validatorUnderTest.validate(ENTITY_PLACEHOLDER.getUuid().toString(), ENTITY_PLACEHOLDER, mav, ManagedAttributeServiceIT.XYZValidationContext.X);
  }

  @Test
  void validate_NonDinaEntity_WhenInvalidIntegerTypeExceptionThrown() {
    testManagedAttribute = ManagedAttributeServiceIT.TestManagedAttribute.builder()
        .name(RandomStringUtils.randomAlphabetic(6))
        .managedAttributeType(ManagedAttribute.ManagedAttributeType.INTEGER)
        .build();
    testManagedAttributeService.create(testManagedAttribute);
    Map<String, String> mav = Map.of(testManagedAttribute.getKey(), "1.2");
    assertThrows(ValidationException.class,
        () ->  validatorUnderTest.validate(ENTITY_PLACEHOLDER.getUuid().toString(), ENTITY_PLACEHOLDER, mav, ManagedAttributeServiceIT.XYZValidationContext.X));
  }

}
