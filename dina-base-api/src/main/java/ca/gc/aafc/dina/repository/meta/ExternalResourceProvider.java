package ca.gc.aafc.dina.repository.meta;

import ca.gc.aafc.dina.dto.ExternalRelationDto;

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
   * Returns a set of external DTO types to be supported.
   *
   * @return a set of external DTO types.
   */
  Set<Class<? extends ExternalRelationDto>> getClasses();

}
