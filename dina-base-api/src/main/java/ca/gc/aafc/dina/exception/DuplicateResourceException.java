package ca.gc.aafc.dina.exception;

import java.util.Objects;
import java.util.UUID;

import lombok.Getter;

@Getter
public final class DuplicateResourceException extends RuntimeException {
  private final String resourceType;
  private final String sourcePointer;

  public static DuplicateResourceException create(String resourceType, UUID duplicateResourceId, String sourcePointer) {
    return new DuplicateResourceException(resourceType, Objects.toString(duplicateResourceId), sourcePointer);
  }

  public static DuplicateResourceException create(String resourceType, String duplicateResourceId, String sourcePointer) {
    return new DuplicateResourceException(resourceType, duplicateResourceId, sourcePointer);
  }

  private DuplicateResourceException(String resourceType, String duplicateResourceId, String sourcePointer) {
    super("Duplicate " + resourceType + " detected. Existing resource ID: " + duplicateResourceId);
    this.resourceType = resourceType;
    this.sourcePointer = sourcePointer;
  }
}
