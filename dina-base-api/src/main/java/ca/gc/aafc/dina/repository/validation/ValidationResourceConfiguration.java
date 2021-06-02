package ca.gc.aafc.dina.repository.validation;

import java.util.List;

/**
 * Configuration design to return a list of {@link javax.xml.validation.ValidatorHandler} to be used by the
 * validation repository.
 */
public interface ValidationResourceConfiguration {

  /**
   * Returns a list of @link javax.xml.validation.ValidatorHandler}
   *
   * @return a list of @link javax.xml.validation.ValidatorHandler}
   */
  List<ValidationResourceHandler<?>> getValidationHandlers();

}
