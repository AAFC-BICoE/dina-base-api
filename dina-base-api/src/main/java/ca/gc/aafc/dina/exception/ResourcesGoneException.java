package ca.gc.aafc.dina.exception;

import java.util.Map;
import lombok.Getter;

/**
 * Similar to {@link ResourceGoneException} but for multiple resources.
 */
@Getter
public final class ResourcesGoneException extends Exception {

  private final String resourceType;
  private final Map<String, String> identifierLinks;

  public static ResourcesGoneException create(String resourceType,
                                              Map<String, String> identifierLinks) {
    return new ResourcesGoneException(resourceType, identifierLinks);
  }

  private ResourcesGoneException(String resourceType, Map<String, String> identifierLinks) {
    super(resourceType + " Gone");
    this.resourceType = resourceType;
    this.identifierLinks = identifierLinks;
  }
}
