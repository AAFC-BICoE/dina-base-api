package ca.gc.aafc.dina.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.validation.ValidationException;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.i18n.MultilingualDescription;

@SpringBootTest(classes = TestDinaBaseApp.class,
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class MultilingualDescriptionValidatorTest {

  @Inject
  private MultilingualDescriptionValidator multilingualDescriptionValidator;

  @Test
  void  validate_WhenValidMultilingualDescription_NoExceptionThrown() {
    List<Map<String, String>> multilingualPair = new ArrayList<Map<String, String>>() {
      {
        add(Map.of("lang", "fr", "desc", "description en Français"));
        add(Map.of("lang", "en", "desc", "description in English"));
      }
    };
    MultilingualDescription multilingualDescription = MultilingualDescription.builder()
      .multilingualPair(multilingualPair)
      .build();


    Errors errors = new BeanPropertyBindingResult(multilingualDescription, MultilingualDescription.class.getSimpleName());
    multilingualDescriptionValidator.validate(multilingualDescription, errors);
    ValidationErrorsHelper.errorsToValidationException(errors);

    assertFalse(errors.hasErrors());
  }

  @Test
  void  validate_WhenInvalidValidMultilingualDescription_ExceptionThrown() {
    List<Map<String, String>> multilingualPair = new ArrayList<Map<String, String>>() {
      {
        add(Map.of("lang", "es", "desc", "description en español"));
      }
    };
    MultilingualDescription multilingualDescription = MultilingualDescription.builder()
      .multilingualPair(multilingualPair)
      .build();


    Errors errors = new BeanPropertyBindingResult(multilingualDescription, MultilingualDescription.class.getSimpleName());
    multilingualDescriptionValidator.validate(multilingualDescription, errors);
    assertThrows(ValidationException.class, () ->
      ValidationErrorsHelper.errorsToValidationException(errors));
  }
  
  
}
