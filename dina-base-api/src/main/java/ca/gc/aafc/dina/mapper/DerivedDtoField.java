package ca.gc.aafc.dina.mapper;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Marks a DTO field that it is read-only and its value should not be copied
 * over to an Entity.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface DerivedDtoField {
}
