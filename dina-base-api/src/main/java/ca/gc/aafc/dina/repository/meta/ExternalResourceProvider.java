package ca.gc.aafc.dina.repository.meta;

/**
 * Provides a mechanism to map external types to their references.
 */
public interface ExternalResourceProvider {

  /**
   * Returns a reference to a given type
   *
   * @param type - type to map to a reference
   * @return a reference to a given type
   */
  String getReferenceForType(String type);

}
