package ca.gc.aafc.dina.exception;

import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

@Getter
public final class ResourceNotFoundException extends Exception {

  private final String resourceType;
  private final String identifier;

  public static ResourceNotFoundException create(String resourceType, UUID identifier) {
    return new ResourceNotFoundException(resourceType, Objects.toString(identifier));
  }

  public static ResourceNotFoundException create(String resourceType, String identifier) {
    return new ResourceNotFoundException(resourceType, identifier);
  }

  private ResourceNotFoundException(String resourceType, String identifier) {
    super(resourceType + " with ID " + identifier + " Not Found");
    this.resourceType = resourceType;
    this.identifier = identifier;
  }
}
