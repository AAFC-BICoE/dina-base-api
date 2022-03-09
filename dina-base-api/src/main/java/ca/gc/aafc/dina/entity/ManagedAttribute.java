package ca.gc.aafc.dina.entity;

import ca.gc.aafc.dina.i18n.MultilingualDescription;

/**
 * Main interface that represents a managed attribute in Dina.
 *
 */
public interface ManagedAttribute extends DinaEntity {

  enum ManagedAttributeType {
    INTEGER, STRING, DATE, BOOL
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

  MultilingualDescription getMultilingualDescription();

}
