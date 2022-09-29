package ca.gc.aafc.dina.i18n;

import ca.gc.aafc.dina.validation.ValidationErrorsHelper;
import org.junit.jupiter.api.Test;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;

import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MultilingualTest {

  @Test
  public void multilingualDescription_onDescription_hasBlankDescriptionReturnsRightResult() {
    MultilingualDescription multilingualDescription = MultilingualDescription.builder()
        .descriptions(List.of(
            MultilingualDescription.MultilingualPair.of("en", "en"),
            MultilingualDescription.MultilingualPair.of("fr", "")))
        .build();
    assertTrue(multilingualDescription.hasBlankDescription());

    // if the list of description is empty it should return false since there is no description to validate
    assertFalse(MultilingualDescription.builder().build().hasBlankDescription());
  }

  @Test
  public void multilingualTitle_onTitle_hasBlankDescriptionReturnsRightResult() {
    MultilingualTitle multilingualTitle = MultilingualTitle.builder()
            .titles(List.of(
                    MultilingualTitle.MultilingualTitlePair.of("en", "en"),
                    MultilingualTitle.MultilingualTitlePair.of("fr", "")))
            .build();
    assertTrue(multilingualTitle.hasBlankTitle());

    // if the list of description is empty it should return false since there is no description to validate
    assertFalse(MultilingualTitle.builder().build().hasBlankTitle());
  }

  @Test
  public void multilingualTitle_onInvalidValues_beanValidationCatchError() {
    MultilingualTitle multilingualTitle = MultilingualTitle.builder()
            .titles(List.of(
                    MultilingualTitle.MultilingualTitlePair.of("en", "en"),
                    MultilingualTitle.MultilingualTitlePair.of("fr", "")))
            .build();

    try (ValidatorFactory vf = Validation.buildDefaultValidatorFactory()) {
      Validator defaultValidator= new SpringValidatorAdapter(vf.getValidator());

      // test with a blank description
      Errors errors = ValidationErrorsHelper.newErrorsObject("abc", multilingualTitle);
      defaultValidator.validate(multilingualTitle, errors);
      assertEquals(1,errors.getAllErrors().size());

      // test with an invalid language code
      multilingualTitle = MultilingualTitle.builder()
              .titles(List.of(
                      MultilingualTitle.MultilingualTitlePair.of("en", "en"),
                      MultilingualTitle.MultilingualTitlePair.of("french", "fr")))
              .build();
      errors = ValidationErrorsHelper.newErrorsObject("abc", multilingualTitle);
      defaultValidator.validate(multilingualTitle, errors);
      assertEquals(1,errors.getAllErrors().size());
    }
  }

  @Test
  public void multilingualDescription_onInvalidValues_beanValidationCatchError() {
    MultilingualDescription multilingualTitle = MultilingualDescription.builder()
            .descriptions(List.of(
                    MultilingualDescription.MultilingualPair.of("en", "en"),
                    MultilingualDescription.MultilingualPair.of("fr", "")))
            .build();

    try (ValidatorFactory vf = Validation.buildDefaultValidatorFactory()) {
      Validator defaultValidator= new SpringValidatorAdapter(vf.getValidator());

      // test with a blank description
      Errors errors = ValidationErrorsHelper.newErrorsObject("abc", multilingualTitle);
      defaultValidator.validate(multilingualTitle, errors);
      assertEquals(1,errors.getAllErrors().size());

      // test with an invalid language code
      multilingualTitle = MultilingualDescription.builder()
              .descriptions(List.of(
                      MultilingualDescription.MultilingualPair.of("en", "en"),
                      MultilingualDescription.MultilingualPair.of("french", "fr")))
              .build();
      errors = ValidationErrorsHelper.newErrorsObject("abc", multilingualTitle);
      defaultValidator.validate(multilingualTitle, errors);
      assertEquals(1,errors.getAllErrors().size());
    }
  }

}
