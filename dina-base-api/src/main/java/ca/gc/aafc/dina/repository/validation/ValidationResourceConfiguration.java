package ca.gc.aafc.dina.repository.validation;

import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.service.DinaService;

import java.util.Set;

public interface ValidationResourceConfiguration {

  /**
   * Returns a service to a given type
   *
   * @param type - type to map to a service
   * @return a service to a given type
   */
    DinaService<? extends DinaEntity> getServiceForType(String type);

  /**
   * Returns a Resource Class to a given type
   *
   * @param type - type to map to a resource class
   * @return a resource class to a given type
   */
  Class<?> getResourceClassForType(String type);

  /**
   * Returns an entity class to a given type
   *
   * @param type - type to map to an entity class
   * @return an entity class to a given type
   */
  Class<? extends DinaEntity> getEntityClassForType(String type);

  /**
   * Returns a set of the supported types
   *
   * @return a set of the supported types
   */
  Set<String> getTypes();

}
