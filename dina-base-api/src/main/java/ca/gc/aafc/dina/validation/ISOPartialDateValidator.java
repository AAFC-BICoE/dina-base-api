package ca.gc.aafc.dina.validation;

import java.time.format.DateTimeParseException;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import ca.gc.aafc.dina.datetime.ISODateTime;

public class ISOPartialDateValidator implements ConstraintValidator<ISOPartialDate, String> {

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    try {
      if (value != null) {
        ISODateTime.parse(value);
      }
      return true;
    } catch (DateTimeParseException e) {
      return false;
    }
  }
  
}
