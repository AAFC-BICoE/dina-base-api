package ca.gc.aafc.dina.exception;

import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

@Getter
public final class ConflictException extends Exception {
  private final String resourceType;
  private final String identifier;

  public static ConflictException create(String resourceType, UUID identifier) {
    return new ConflictException(resourceType, Objects.toString(identifier));
  }

  public static ConflictException create(String resourceType, String identifier) {
    return new ConflictException(resourceType, identifier);
  }

  private ConflictException(String resourceType, String identifier) {
    super(resourceType + " with ID " + identifier + " in Conflict");
    this.resourceType = resourceType;
    this.identifier = identifier;
  }
}
