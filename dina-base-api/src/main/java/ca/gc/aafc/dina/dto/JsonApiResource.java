package ca.gc.aafc.dina.dto;

import java.util.UUID;

/**
 * Interface of a resource (usually DTO) that can be exposed in JSON:API.
 *
 */
public interface JsonApiResource {
  String getJsonApiType();
  UUID getJsonApiId();
}
