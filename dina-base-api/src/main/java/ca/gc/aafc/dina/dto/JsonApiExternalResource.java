package ca.gc.aafc.dina.dto;

import java.util.UUID;

/**
 * Interface representing a resource held in another module.
 */
public interface JsonApiExternalResource {
  String getJsonApiType();
  UUID getJsonApiId();
}
