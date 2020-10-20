package ca.gc.aafc.dina.repository.meta;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Used to mark a Relation as an External Relation
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonApiExternalRelation {

  /**
   * the resource type of the relation
   *
   * @return the resource type of the relation
   */
  String type();
}
