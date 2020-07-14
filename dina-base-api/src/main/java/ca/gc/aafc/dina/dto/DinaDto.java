package ca.gc.aafc.dina.dto;

/**
 * Used for Dina spring security, Ensures repo domain objects have a group used
 * to restrict access permissions.
 */
public interface DinaDto {

  /**
   * The group represents the group owning the entity. group is optional and null
   * is return if an entity doesn't support it.
   * 
   * @return the name of the group or null
   */
  default String getGroup() {
    return null;
  }

}
