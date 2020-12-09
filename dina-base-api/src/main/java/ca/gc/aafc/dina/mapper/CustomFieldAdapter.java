package ca.gc.aafc.dina.mapper;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class to map fields with a given set of adapters.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CustomFieldAdapter {

  /**
   * Returns the adapters used to map this class.
   *
   * @return Returns the adapters used to map this class.
   */
  Class<? extends DinaFieldAdapter<?, ?, ?, ?>>[] adapters();

}
