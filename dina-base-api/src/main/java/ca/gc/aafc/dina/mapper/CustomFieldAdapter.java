package ca.gc.aafc.dina.mapper;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field to be mapped using a {@link DinaFieldAdapter}
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CustomFieldAdapter {

  /**
   * Returns the adapter used to map this field.
   *
   * @return the adapter used to map this field.
   */
  Class<? extends DinaFieldAdapter<?, ?, ?, ?>> adapter();

}
