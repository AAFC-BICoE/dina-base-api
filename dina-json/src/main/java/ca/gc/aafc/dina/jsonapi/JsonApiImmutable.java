package ca.gc.aafc.dina.jsonapi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Identify immutability of field on a DTO to control some values that can not be created or updated.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonApiImmutable {

  enum ImmutableOn { CREATE, UPDATE }

  ImmutableOn[] value() default {};
}
