package ca.gc.aafc.dina.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = ISOPartialDateValidator.class)
public @interface ISOPartialDate {
  
  String message() default "The provided value cannot be parsed to ISO Date";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
