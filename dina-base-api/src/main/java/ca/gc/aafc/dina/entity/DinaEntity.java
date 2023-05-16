package ca.gc.aafc.dina.entity;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Represents a DINA entity with an id and uuid field. The id is usually the Primary Key
 * and auto-generated while the uuid is the natural key, a unique key that is publicly visible.
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

  /**
   * Is the entity considered publicly releasable?
   * Optional.empty() means inapplicable or unknown and needs to be interpreted by the caller.
   *
   * @return Optional with the isPubliclyReleasable flag or Optional.empty() if inapplicable or unknown
   */
  default Optional<Boolean> isPubliclyReleasable() {
    return Optional.empty();
  }

  String getCreatedBy();

  OffsetDateTime getCreatedOn();

}
