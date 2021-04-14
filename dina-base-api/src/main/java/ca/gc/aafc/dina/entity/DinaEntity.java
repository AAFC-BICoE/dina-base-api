package ca.gc.aafc.dina.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Represents a DINA entity with an id and uuid field. The id and uuid is usually the Primary Key
 * and auto-generated.
 */
public interface DinaEntity {

  Integer getId();

  UUID getUuid();

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
