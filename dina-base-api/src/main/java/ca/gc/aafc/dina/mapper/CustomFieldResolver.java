package ca.gc.aafc.dina.mapper;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Marks a field is resolved through the use of Custom Field Resolvers.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface CustomFieldResolver {

  enum Direction {

    TO_DTO, TO_ENTITY

  }

  String field();

  Direction getDirection();

}
