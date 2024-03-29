package ca.gc.aafc.dina.validation;

import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.entity.Department;
import ca.gc.aafc.dina.entity.ma.TestManagedAttribute;
import ca.gc.aafc.dina.entity.ma.TestManagedAttributeUsage;
import ca.gc.aafc.dina.i18n.MultilingualDescription;
import ca.gc.aafc.dina.service.ManagedAttributeService;
import ca.gc.aafc.dina.service.ManagedAttributeServiceIT;

import ca.gc.aafc.dina.testsupport.PostgresTestContainerInitializer;
import ca.gc.aafc.dina.vocabulary.TypedVocabularyElement;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.ValidationException;

import com.google.common.collect.ImmutableList;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
@SpringBootTest(classes = {TestDinaBaseApp.class, ManagedAttributeServiceIT.ManagedAttributeConfig.class})
@ContextConfiguration(initializers = { PostgresTestContainerInitializer.class })
public class ManagedAttributeValueValidatorTest {

  private static final TestManagedAttributeUsage ENTITY_PLACEHOLDER = TestManagedAttributeUsage
      .builder()
      .uuid(UUID.randomUUID())
      .build();

  @Inject
  private ManagedAttributeService<TestManagedAttribute> testManagedAttributeService;

  @Inject
  private ManagedAttributeValueValidator<TestManagedAttribute> validatorUnderTest;

  private TestManagedAttribute testManagedAttribute;

  @Test
  void validate_WhenValidDateType_NoExceptionThrown() {
    testManagedAttribute = testManagedAttributeService.create(
        newTestManagedAttribute(TypedVocabularyElement.VocabularyElementType.DATE));

    Map<String, String> mav = Map.of(testManagedAttribute.getKey(), LocalDate.now().toString());
    validatorUnderTest.validate(ENTITY_PLACEHOLDER, mav, ManagedAttributeServiceIT.XYZValidationContext.X);
  }

  @ParameterizedTest
  @ValueSource(strings = {"1.2", "", "  ", "\t", "\n", "a", "99-02-01", "11-23-2020", "11-1999-01", "01-01-01"})
  void validate_WhenInvalidDateType_ExceptionThrown(String value) {
    testManagedAttribute = testManagedAttributeService.create(
        newTestManagedAttribute(TypedVocabularyElement.VocabularyElementType.DATE));

    Map<String, String> mav = Map.of(testManagedAttribute.getKey(), value);
    assertThrows(ValidationException.class, () -> validatorUnderTest.validate(ENTITY_PLACEHOLDER, mav, ManagedAttributeServiceIT.XYZValidationContext.X));
  }

  @Test
  void validate_WhenValidStringType_NoExceptionThrown() {
    testManagedAttribute = TestManagedAttribute.builder().
      name(RandomStringUtils.randomAlphabetic(6)).uuid(UUID.randomUUID())
      .vocabularyElementType(TypedVocabularyElement.VocabularyElementType.STRING)
      .component(ManagedAttributeServiceIT.XYZValidationContext.X.toString())
      .build();
    testManagedAttributeService.create(testManagedAttribute);

    Map<String, String> mav = Map.of(testManagedAttribute.getKey(), "new string value");
    validatorUnderTest.validate(ENTITY_PLACEHOLDER, mav, ManagedAttributeServiceIT.XYZValidationContext.X);
  }

  @Test
  void validate_WhenValidIntegerType_NoExceptionThrown() {
    testManagedAttribute = TestManagedAttribute.builder()
        .name(RandomStringUtils.randomAlphabetic(6)).uuid(UUID.randomUUID())
        .vocabularyElementType(TypedVocabularyElement.VocabularyElementType.INTEGER)
        .component(ManagedAttributeServiceIT.XYZValidationContext.X.toString())
        .build();
    testManagedAttributeService.create(testManagedAttribute);

    Map<String, String> mav = Map.of(testManagedAttribute.getKey(), "1");
    validatorUnderTest.validate(ENTITY_PLACEHOLDER, mav, ManagedAttributeServiceIT.XYZValidationContext.X);
  }

  @ParameterizedTest
  @ValueSource(strings = {"1.2", "", "  ", "\t", "\n", "a"})
  void validate_WhenInvalidIntegerType_ExceptionThrown(String value) {
    testManagedAttribute = TestManagedAttribute.builder()
        .name(RandomStringUtils.randomAlphabetic(6)).uuid(UUID.randomUUID())
        .vocabularyElementType(TypedVocabularyElement.VocabularyElementType.INTEGER)
        .component(ManagedAttributeServiceIT.XYZValidationContext.X.toString())
        .build();
    testManagedAttributeService.create(testManagedAttribute);

    Map<String, String> mav = Map.of(testManagedAttribute.getKey(), value);
    assertThrows(ValidationException.class, () ->
        validatorUnderTest.validate(ENTITY_PLACEHOLDER, mav, ManagedAttributeServiceIT.XYZValidationContext.X));
  }

  @Test
  void validate_WhenInvalidIntegerTypeExceptionThrown() {
    testManagedAttribute = TestManagedAttribute.builder()
        .name(RandomStringUtils.randomAlphabetic(6)).uuid(UUID.randomUUID())
        .component(ManagedAttributeServiceIT.XYZValidationContext.X.toString())
        .vocabularyElementType(TypedVocabularyElement.VocabularyElementType.INTEGER)
        .build();
    testManagedAttributeService.create(testManagedAttribute);
    Map<String, String> mav = Map.of(testManagedAttribute.getKey(), "1.2");
    assertThrows(ValidationException.class,
        () -> validatorUnderTest.validate(Department.builder().uuid(UUID.randomUUID()).build(), mav, ManagedAttributeServiceIT.XYZValidationContext.X));
  }

  @Test
  void validate_WhenPreValidationFailsExceptionThrown() {
    testManagedAttribute = TestManagedAttribute.builder().
        name(RandomStringUtils.randomAlphabetic(6)).uuid(UUID.randomUUID())
        .vocabularyElementType(TypedVocabularyElement.VocabularyElementType.INTEGER)
        .component(ManagedAttributeServiceIT.XYZValidationContext.X.toString())
        .failValidateValue(true)
        .build();
    testManagedAttributeService.create(testManagedAttribute);
    Map<String, String> mav = Map.of(testManagedAttribute.getKey(), "2");
    assertThrows(ValidationException.class,
        () -> validatorUnderTest.validate(ENTITY_PLACEHOLDER, mav, ManagedAttributeServiceIT.XYZValidationContext.X));
  }

  @Test
  public void assignedValueContainedInAcceptedValues_validationPasses() {
    testManagedAttribute = TestManagedAttribute.builder()
        .name("My special Attribute").uuid(UUID.randomUUID())
        .component(ManagedAttributeServiceIT.XYZValidationContext.X.toString())
        .vocabularyElementType(TypedVocabularyElement.VocabularyElementType.STRING)
        .acceptedValues(new String[]{"val1", "val2"}).build();
    testManagedAttributeService.create(testManagedAttribute);

    Map<String, String> mav = Map.of(testManagedAttribute.getKey(), "val2");
    validatorUnderTest.validate(ENTITY_PLACEHOLDER, mav, ManagedAttributeServiceIT.XYZValidationContext.X);
  }

  @Test
  public void assignedValueNotContainedInAcceptedValues_validationFails() {
    testManagedAttribute = TestManagedAttribute.builder()
        .name("key2").uuid(UUID.randomUUID())
        .vocabularyElementType(TypedVocabularyElement.VocabularyElementType.STRING)
        .component(ManagedAttributeServiceIT.XYZValidationContext.X.toString())
        .acceptedValues(new String[]{"val1", "val2"}).build();
    testManagedAttributeService.create(testManagedAttribute);

    Map<String, String> mav = Map.of(testManagedAttribute.getKey(), "val3");

    Errors errors = new BeanPropertyBindingResult(mav, "mav");
    validatorUnderTest.validate(mav, errors, ManagedAttributeServiceIT.XYZValidationContext.X);
    assertEquals(1, errors.getErrorCount());

    assertTrue(errors.getAllErrors().get(0).getDefaultMessage().contains("val3"));
  }

  @Test
  public void assignedKeyDoesNotExist_ValidationExceptionThrown() {
    Map<String, String> mav = Map.of("key_x", "val3");
    assertThrows(ValidationException.class, () ->
        validatorUnderTest.validate(ENTITY_PLACEHOLDER, mav, ManagedAttributeServiceIT.XYZValidationContext.X));
  }

  @Test
  public void validate_whenEmpty_NoErrors() {
    Map<String, String> mav = Map.of();
    Errors errors = new BeanPropertyBindingResult(mav, "mav");
    validatorUnderTest.validate(mav, errors, ManagedAttributeServiceIT.XYZValidationContext.X);
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
    testManagedAttribute = testManagedAttributeService.create(
        TestManagedAttribute.builder()
            .name(RandomStringUtils.randomAlphabetic(6)).uuid(UUID.randomUUID())
            .vocabularyElementType(TypedVocabularyElement.VocabularyElementType.DATE)
            .component(ManagedAttributeServiceIT.XYZValidationContext.X.toString()).build());

    Map<String, String> mav = Map.of(testManagedAttribute.getKey(), LocalDate.now().toString());

    validatorUnderTest.validate(ENTITY_PLACEHOLDER.getUuid().toString(), ENTITY_PLACEHOLDER, mav,
        ManagedAttributeServiceIT.XYZValidationContext.X);
  }

  @Test
  void validate_NonDinaEntity_WhenInvalidIntegerTypeExceptionThrown() {
    testManagedAttribute = TestManagedAttribute.builder()
        .name(RandomStringUtils.randomAlphabetic(6)).uuid(UUID.randomUUID())
        .vocabularyElementType(TypedVocabularyElement.VocabularyElementType.INTEGER)
        .build();
    testManagedAttributeService.create(testManagedAttribute);
    Map<String, String> mav = Map.of(testManagedAttribute.getKey(), "1.2");
    assertThrows(ValidationException.class,
        () ->  validatorUnderTest.validate(ENTITY_PLACEHOLDER.getUuid().toString(), ENTITY_PLACEHOLDER, mav, ManagedAttributeServiceIT.XYZValidationContext.X));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "off", "TRUE"})
  void validate_WhenInvalidBoolType_ExceptionThrown(String value) {
    testManagedAttribute = testManagedAttributeService.create(TestManagedAttribute.builder()
        .name(RandomStringUtils.randomAlphabetic(6)).uuid(UUID.randomUUID())
        .component(ManagedAttributeServiceIT.XYZValidationContext.X.toString())
        .vocabularyElementType(TypedVocabularyElement.VocabularyElementType.BOOL)
        .build());
    Map<String, String> mav = Map.of(testManagedAttribute.getKey(), value);
    assertThrows(ValidationException.class, () -> validatorUnderTest.validate(ENTITY_PLACEHOLDER, mav, ManagedAttributeServiceIT.XYZValidationContext.X));
  }

  @ParameterizedTest
  @ValueSource(strings = {"true", "false"})
  void validate_ValidBoolType_NoExceptionThrown(String value) {
    testManagedAttribute = testManagedAttributeService.create(newTestManagedAttribute(
        TypedVocabularyElement.VocabularyElementType.BOOL));

    Map<String, String> mav = Map.of(testManagedAttribute.getKey(), value);
    validatorUnderTest.validate(ENTITY_PLACEHOLDER, mav, ManagedAttributeServiceIT.XYZValidationContext.X);
  }

  @ParameterizedTest
  @ValueSource(strings = {"a.3", "A", "-2.3a", "2,3"})
  void validate_InvalidDecimalType_ExceptionThrown(String value) {
    testManagedAttribute = testManagedAttributeService.create(newTestManagedAttribute(
            TypedVocabularyElement.VocabularyElementType.DECIMAL));

    Map<String, String> mav = Map.of(testManagedAttribute.getKey(), value);
    assertThrows(ValidationException.class, () -> validatorUnderTest.validate(ENTITY_PLACEHOLDER, mav, ManagedAttributeServiceIT.XYZValidationContext.X));
  }

  @ParameterizedTest
  @ValueSource(strings = {"0.3", "123.543", "-2.3", "2"})
  void validate_ValidDecimalType_NoExceptionThrown(String value) {
    testManagedAttribute = testManagedAttributeService.create(newTestManagedAttribute(
            TypedVocabularyElement.VocabularyElementType.DECIMAL));

    Map<String, String> mav = Map.of(testManagedAttribute.getKey(), value);
    validatorUnderTest.validate(ENTITY_PLACEHOLDER, mav, ManagedAttributeServiceIT.XYZValidationContext.X);
  }

  @Test
  void validate_duplicatedKeysDifferentContext_ValidationWorks() {
    TestManagedAttribute testManagedAttribute1 = newTestManagedAttribute(TypedVocabularyElement.VocabularyElementType.BOOL);
    testManagedAttribute1.setName("name1");
    testManagedAttribute1.setComponent(ManagedAttributeServiceIT.XYZValidationContext.X.toString());
    testManagedAttribute1 = testManagedAttributeService.createAndFlush(testManagedAttribute1);

    // same name but on a different component
    TestManagedAttribute testManagedAttribute2 = newTestManagedAttribute(TypedVocabularyElement.VocabularyElementType.BOOL);
    testManagedAttribute2.setName("name1");
    testManagedAttribute2.setComponent(ManagedAttributeServiceIT.XYZValidationContext.Y.toString());
    testManagedAttributeService.createAndFlush(testManagedAttribute2);

    Map<String, String> mav = Map.of(testManagedAttribute1.getKey(), "true");
    validatorUnderTest.validate(ENTITY_PLACEHOLDER, mav, ManagedAttributeServiceIT.XYZValidationContext.X);
  }

  private TestManagedAttribute newTestManagedAttribute(TypedVocabularyElement.VocabularyElementType type) {
    return TestManagedAttribute.builder().
        name(RandomStringUtils.randomAlphabetic(6)).uuid(UUID.randomUUID())
        .vocabularyElementType(type)
        .component(ManagedAttributeServiceIT.XYZValidationContext.X.toString())
        .unit(RandomStringUtils.randomAlphabetic(3))
        .multilingualDescription(MultilingualDescription.builder()
            .descriptions(ImmutableList.of(
                MultilingualDescription.MultilingualPair.of("en", "test"))
            )
            .build())
        .build();
  }

}
