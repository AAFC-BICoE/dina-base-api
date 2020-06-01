package ca.gc.aafc.dina.mapper;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field is resolved through the use of Custom Field Resolvers.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CustomFieldResolver {

  enum Direction {

    TO_DTO, TO_ENTITY

  }

  String field();

  Direction getDirection();

}
