package ca.gc.aafc.dina.exception;

import java.util.UUID;
import lombok.Getter;

@Getter
public class ResourceNotFoundException extends Exception {

  private final String identifier;

  public ResourceNotFoundException(String identifier, Throwable cause) {
    super(cause);
    this.identifier = identifier;
  }

  public ResourceNotFoundException(UUID identifier) {
    this(identifier.toString());
  }

  public ResourceNotFoundException(String identifier) {
    this.identifier = identifier;
  }
}
