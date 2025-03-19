package ca.gc.aafc.dina.exception;

import java.util.UUID;
import lombok.Getter;

@Getter
public final class ResourceGoneException extends Exception {

  private final String resourceType;
  private final String identifier;
  private final String link;

  public static ResourceGoneException create(String resourceType, UUID identifier, String link) {
    return new ResourceGoneException(resourceType, identifier, link);
  }

  private ResourceGoneException(String resourceType, UUID identifier, String link) {

    super(resourceType + " with ID " + identifier.toString() + " Gone."
      + " The Resource has been deleted but audit records remain, see the links.about section");
    this.resourceType = resourceType;
    this.identifier = identifier.toString();
    this.link = link;
  }
}
