package ca.gc.aafc.dina.dto;

/**
 * Represents a typical DTI in DINA.
 */
public interface DinaDto extends JsonApiResource{

  /**
   * Used by resource that keeps track of version of the resource to detect stale client data.
   * optional
   * @return the version or null if not implemented or not provided.
   */
  default Long getResourceVersion() {
    return null;
  }
}
