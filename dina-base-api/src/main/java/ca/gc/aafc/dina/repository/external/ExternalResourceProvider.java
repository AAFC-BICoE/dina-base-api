package ca.gc.aafc.dina.repository.external;

import java.util.Set;

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

  /**
   * Returns a set of the supported types
   *
   * @return a set of the supported types
   */
  Set<String> getTypes();

}
