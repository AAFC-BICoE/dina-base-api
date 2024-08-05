package ca.gc.aafc.dina.entity;

/**
 * Indicates that an entity is identifiable by name.
 * Uniqueness of the name is decided by the implementation.
 */
public interface IdentifiableByName {

  String getName();
  void setName(String name);
}
