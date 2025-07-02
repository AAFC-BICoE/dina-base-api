package ca.gc.aafc.dina.exception;

import java.util.List;
import lombok.Getter;

/**
 * Similar to {@link ResourceNotFoundException} but for multiple resources.
 */
@Getter
public final class ResourcesNotFoundException extends Exception {

  private final String resourceType;
  private final List<String> identifier;

  public static ResourcesNotFoundException create(String resourceType, List<String> identifier) {
    return new ResourcesNotFoundException(resourceType, identifier);
  }

  private ResourcesNotFoundException(String resourceType, List<String> identifiers) {
    super(resourceType + " Not Found");
    this.resourceType = resourceType;
    this.identifier = identifiers;
  }
}
