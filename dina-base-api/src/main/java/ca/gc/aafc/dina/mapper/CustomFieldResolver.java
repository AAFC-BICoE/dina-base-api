package ca.gc.aafc.dina.mapper;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method is used to resolve a custom field mapping.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CustomFieldResolver {

  /**
   * Field Name of the field the custom field resolver will map. Case sensitive.
   * 
   * @return Field Name of the field
   */
  String fieldName();

}
