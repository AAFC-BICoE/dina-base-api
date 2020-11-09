package ca.gc.aafc.dina.mapper;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field to be resolved using a custom field mapping indicated by the annotations given
 * custom setter method. The setter method refers to the method will set the apply the custom
 * mapping. A field resolver should always have a parameter type that matches the source class of
 * the mapping. A field resolver return types must also match the mapping target field type.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CustomFieldResolver {

  /**
   * Name of the method that will apply the custom field mapping. A field resolver should always
   * have a parameter type that matches the source class of the mapping. A field resolver return
   * types must also match the mapping target field type.
   *
   * @return Name of the setter method.
   */
  String setterMethod();

}

