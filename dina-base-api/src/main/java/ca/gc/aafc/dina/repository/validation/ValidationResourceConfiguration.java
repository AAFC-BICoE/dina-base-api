package ca.gc.aafc.dina.repository.validation;

import java.util.Set;

import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.service.DefaultDinaService;

public interface ValidationResourceConfiguration<D, E extends DinaEntity> {
  
/**
   * Returns a servive to a given type
   * 
   * @param type - type to map to a service
   * @return a service to a given type
   */
  DefaultDinaService<E> getServiceForType(String type);

  Class<E> getEntityClassForType(String type);

  Class<D> getResourceClassForType(String type);

  /**
   * Returns a set of the supported types
   * 
   * @return a set of the supported types
   */
  Set<String> getTypes();

}
