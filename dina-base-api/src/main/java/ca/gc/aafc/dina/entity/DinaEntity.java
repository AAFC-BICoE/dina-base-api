package ca.gc.aafc.dina.entity;

import java.time.OffsetDateTime;

/**
 * Represents a DINA entity with an id field. The id is usually the Primary Key
 * and auto-generated.
 */
public interface DinaEntity {

  Integer getId();

  /**
   * The group represents the group owning the entity. group is optional and null
   * is return if an entity doesn't support it.
   * 
   * @return the name of the group or null
   */
  default String getGroup() {
    return null;
  }

    String getCreatedBy();

    OffsetDateTime getCreatedOn();

}
