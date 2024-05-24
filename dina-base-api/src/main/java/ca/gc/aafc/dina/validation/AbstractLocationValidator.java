package ca.gc.aafc.dina.validation;

import javax.inject.Named;

import ca.gc.aafc.dina.entity.StorageGridLayout;
import ca.gc.aafc.dina.translator.NumberLetterTranslator;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

public abstract class AbstractLocationValidator implements Validator {

  public static final String VALID_NULL_WELL_COL = "location.nullWellCol";
  public static final String VALID_NULL_WELL_ROW = "location.nullWellRow";

  public static final String INVALID_ROW = "location.invalidWellRow";
  public static final String INVALID_COL = "location.invalidWellColumn";

  private final MessageSource messageSource;

  // could become variables if needed in the future
  protected static final String WELL_ROW_FIELD_NAME = "wellRow";
  protected static final String WELL_COLUMN_FIELD_NAME = "wellColumn";

  protected AbstractLocationValidator(@Named("validationMessageSource") MessageSource messageSource) {
    this.messageSource = messageSource;
  }

  /**
   * Validate that column and row are provided together, as a pair.
   * @param row
   * @param col
   * @param errors
   */
  protected void checkRowAndColumn(String row, Integer col, Errors errors) {
    // Row and col must be either both set or both null.
    if (StringUtils.isBlank(row) && col != null) {
      String errorMessage = getMessage(VALID_NULL_WELL_ROW);
      errors.rejectValue(WELL_ROW_FIELD_NAME, VALID_NULL_WELL_ROW, errorMessage);
    }
    if (col == null && StringUtils.isNotBlank(row)) {
      String errorMessage = getMessage(VALID_NULL_WELL_COL);
      errors.rejectValue(WELL_COLUMN_FIELD_NAME, VALID_NULL_WELL_COL, errorMessage);
    }
  }

  protected void checkWellAgainstGrid(String row, Integer col, StorageGridLayout sgl, Errors errors) {
    // Validate well coordinates if they are set.
    if (row != null && col != null) {
      if (!sgl.isValidRow(NumberLetterTranslator.toNumber(row))) {
        String errorMessage = getMessage(INVALID_ROW, row );
        // we need to attach the error to a specific field but here it could be col OR row
        errors.rejectValue(WELL_ROW_FIELD_NAME, INVALID_ROW, errorMessage);
      }
      if (!sgl.isValidColumn(col)) {
        String errorMessage = getMessage(INVALID_COL, col);
        // we need to attach the error to a specific field but here it could be col OR row
        errors.rejectValue(WELL_COLUMN_FIELD_NAME, INVALID_COL, errorMessage);
      }
    }
  }

  private String getMessage(String key, Object... objects) {
    return messageSource.getMessage(key, objects, LocaleContextHolder.getLocale());
  }
}
