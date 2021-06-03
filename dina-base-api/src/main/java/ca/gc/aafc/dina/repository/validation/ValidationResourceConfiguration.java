package ca.gc.aafc.dina.repository.validation;

import java.util.List;

/**
 * Configuration design to return a list of {@link ValidationResourceHandler} to be used by the
 * validation repository.
 */
public interface ValidationResourceConfiguration {

  /**
   * Returns a list of {@link ValidationResourceHandler}
   *
   * @return a list of {@link ValidationResourceHandler}
   */
  List<ValidationResourceHandler<?>> getValidationHandlers();

}
