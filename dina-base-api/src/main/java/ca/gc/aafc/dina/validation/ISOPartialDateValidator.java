package ca.gc.aafc.dina.validation;

import java.time.format.DateTimeParseException;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import ca.gc.aafc.dina.datetime.ISODateTime;
import ca.gc.aafc.dina.datetime.ISODateTime.Format;

public class ISOPartialDateValidator implements ConstraintValidator<ISOPartialDate, String> {

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    try {
      if (value != null) {
        ISODateTime isoDateTime = ISODateTime.parse(value);
        Format format = isoDateTime.getFormat();
        return format.getPrecision() <= Format.YYYY_MM_DD.getPrecision();
      }
      return true;
    } catch (DateTimeParseException e) {
      return false;
    }
  }
  
}
