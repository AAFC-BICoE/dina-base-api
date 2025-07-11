package ca.gc.aafc.dina.exception;

/**
 * DINA specific {@link RuntimeException} that indicates that an attribute can not be found on a specific object.
 */
public class UnknownAttributeException extends RuntimeException {

  public UnknownAttributeException(Throwable cause) {
    super(cause);
  }

  public UnknownAttributeException(String attributeName) {
    super(attributeName + " : unknown attribute");
  }
}
