package ca.gc.aafc.dina.repository.validation;

import java.util.Set;

import org.springframework.validation.Validator;

import ca.gc.aafc.dina.entity.DinaEntity;

public interface ValidationResourceConfiguration {

  /**
   * Returns a validator to a given type
   *
   * @param type - type to map to a Validator
   * @return a validator to a given type
   */
  Validator getValidatorForType(String type);

  /**
   * Returns a Resource Class to a given type
   *
   * @param type - type to map to a resource class
   * @return a resource class to a given type
   */
   <D> Class<D> getResourceClassForType(String type);

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
