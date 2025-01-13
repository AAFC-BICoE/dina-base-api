package ca.gc.aafc.dina.exception;

import java.util.UUID;
import lombok.Getter;

@Getter
public final class ResourceNotFoundException extends Exception {

  private final String resourceType;
  private final String identifier;

  public static ResourceNotFoundException create(String resourceType, UUID identifier) {
    return new ResourceNotFoundException(resourceType, identifier);
  }

  private ResourceNotFoundException(String resourceType, UUID identifier) {
    super(resourceType + " with ID " + identifier.toString() + " Not Found");
    this.resourceType = resourceType;
    this.identifier = identifier.toString();
  }
}
