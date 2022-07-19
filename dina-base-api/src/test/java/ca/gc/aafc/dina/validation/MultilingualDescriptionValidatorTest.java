package ca.gc.aafc.dina.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import javax.inject.Inject;
import javax.validation.ValidationException;

import ca.gc.aafc.dina.testsupport.PostgresTestContainerInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.i18n.MultilingualDescription;
import ca.gc.aafc.dina.i18n.MultilingualDescription.MultilingualPair;

@SpringBootTest(classes = TestDinaBaseApp.class,
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = { PostgresTestContainerInitializer.class })
public class MultilingualDescriptionValidatorTest {

  @Inject
  private MultilingualDescriptionValidator multilingualDescriptionValidator;

  @Test
  void  validate_WhenValidMultilingualDescription_NoExceptionThrown() {
    MultilingualPair multilingualPair_fr =
        MultilingualPair.of("fr", "description en français");

    MultilingualPair multilingualPair_en =
        MultilingualPair.of("en", "description in English");

    MultilingualDescription multilingualDescription = MultilingualDescription.builder()
      .descriptions(List.of(multilingualPair_fr, multilingualPair_en))
      .build();


    Errors errors = new BeanPropertyBindingResult(multilingualDescription, MultilingualDescription.class.getSimpleName());
    multilingualDescriptionValidator.validate(multilingualDescription, errors);
    ValidationErrorsHelper.errorsToValidationException(errors);

    assertFalse(errors.hasErrors());
  }

  @Test
  void  validate_WhenInvalidValidMultilingualDescription_ExceptionThrown() {
    MultilingualPair multilingualPair_es =
        MultilingualPair.of("es", "escription en español");

    MultilingualDescription multilingualDescription = MultilingualDescription.builder()
      .descriptions(List.of(multilingualPair_es))
      .build();

    Errors errors = new BeanPropertyBindingResult(multilingualDescription, MultilingualDescription.class.getSimpleName());
    multilingualDescriptionValidator.validate(multilingualDescription, errors);
    assertThrows(ValidationException.class, () ->
      ValidationErrorsHelper.errorsToValidationException(errors));
  }
  
}
