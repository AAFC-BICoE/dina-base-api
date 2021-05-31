package ca.gc.aafc.dina.repository.validation;

import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.repository.DinaRepository;

import java.util.Set;

public interface ValidationResourceConfiguration {

  /**
   * Returns a service to a given type
   *
   * @param type - type to map to a service
   * @return a service to a given type
   */
  DinaRepository<?, ? extends DinaEntity> getRepoForType(Class<?> type);

  /**
   * Returns a set of the supported resource types
   *
   * @return a set of the supported resource types
   */
  Set<Class<?>> getTypes();

}
