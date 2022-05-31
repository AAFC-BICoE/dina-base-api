package ca.gc.aafc.dina.validation;

/**
 * Marker Interface used to send some context to a validation
 */
public interface ValidationContext {

  /**
   * Value of the context as an Object.
   * Can be used to add to a JPA Criteria.
   * @return
   */
  Object getValue();
}
