package ca.gc.aafc.dina.validation;

import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.validation.Errors;

import ca.gc.aafc.dina.entity.StorageGridLayout;

import lombok.AllArgsConstructor;
import lombok.Getter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AbstractStorageLocationValidatorTest {

  /**
   * Build a MessageSource to avoid loading the entire SpringContext only for that
   */
  private MessageSource buildValidationMessageSource() {
    ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
    messageSource.setAlwaysUseMessageFormat(true);

    messageSource.setBasename("classpath:base-validation-messages");
    messageSource.setDefaultEncoding("UTF-8");
    return messageSource;
  }

  @Test
  public void storageLocation_onInvalidCoordinates_exception() {

    StorageGridLayout sgl = StorageGridLayout.builder()
      .numberOfRows(8)
      .numberOfColumns(8)
      .fillDirection(StorageGridLayout.FillDirection.BY_ROW)
      .build();

    TestStorageLocation tsl = new TestStorageLocation( "A", 2, sgl);

    TestValidator testValidator = new TestValidator(buildValidationMessageSource());
    Errors errors = ValidationErrorsHelper.newErrorsObject("123", tsl);
    testValidator.validate(tsl, errors);
    assertFalse(errors.hasErrors());

    tsl = new TestStorageLocation( "A", 14, sgl);
    errors = ValidationErrorsHelper.newErrorsObject("124", tsl);
    testValidator.validate(tsl, errors);
    assertTrue(errors.hasErrors());
  }

  @Getter
  @AllArgsConstructor
  public static class TestStorageLocation {
    //private String id;
    private String wellRow;
    private Integer wellColumn;
    private StorageGridLayout sgl;
  }

  public static class TestValidator extends AbstractStorageLocationValidator {

    protected TestValidator(MessageSource messageSource) {
      super(messageSource);
    }

    @Override
    public boolean supports(Class<?> clazz) {
      return TestStorageLocation.class == clazz;
    }

    @Override
    public void validate(Object target, Errors errors) {
      if(supports(target.getClass())) {
        TestStorageLocation tsl =  (TestStorageLocation)target;
        checkRowAndColumn(tsl.getWellRow(), tsl.getWellColumn(), errors);
        checkWellAgainstGrid(tsl.getWellRow(), tsl.getWellColumn(), tsl.getSgl(), errors);
      }
    }
  }

}
