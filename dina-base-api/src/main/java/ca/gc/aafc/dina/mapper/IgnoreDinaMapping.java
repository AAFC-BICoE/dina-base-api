package ca.gc.aafc.dina.mapper;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Marks a DTO field that it is to be skipped by the dina mapper.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface IgnoreDinaMapping {

  String reason() default "";

}
