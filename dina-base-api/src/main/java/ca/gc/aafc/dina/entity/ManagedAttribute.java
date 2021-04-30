package ca.gc.aafc.dina.entity;

/**
 * Main interface that represents a managed attribute in Dina.
 *
 */
public interface ManagedAttribute extends DinaEntity {

  enum ManagedAttributeType {
    INTEGER, STRING
  }

  String getName();

  /**
   * Immutable key representing the managed attribute.
   *
   * @return
   */
  String getKey();
  void setKey(String key);

  ManagedAttributeType getManagedAttributeType();
  String[] getAcceptedValues();

}
