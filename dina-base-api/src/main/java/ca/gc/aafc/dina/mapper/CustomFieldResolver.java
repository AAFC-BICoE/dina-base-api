package ca.gc.aafc.dina.mapper;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method is used to resolve a custom field mapping. On a class to be
 * used by dina mapper where fieldName refers to a field on the class where the
 * annotation is used.
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
