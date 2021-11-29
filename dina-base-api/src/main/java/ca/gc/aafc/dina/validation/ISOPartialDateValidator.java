package ca.gc.aafc.dina.validation;

import java.util.Optional;
import java.util.regex.Pattern;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import ca.gc.aafc.dina.datetime.ISODateTime.Format;

public class ISOPartialDateValidator implements ConstraintValidator<ISOPartialDate, String> {

  private static final Pattern ALL_NON_NUMERIC = Pattern.compile("[^\\d]");

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (value != null) {
      int numberOfNumericChar = ALL_NON_NUMERIC
        .matcher(value).replaceAll("").length();

      Optional<Format> format = Format.fromPrecision(numberOfNumericChar);
  
      return format.isPresent() && format.get().getPrecision() <= Format.YYYY_MM_DD.getPrecision();
    } else {
      return true;
    }
  }
  
}
