package ca.gc.aafc.dina.jsonapi;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Jacksonized
@Builder
@Getter
public class JsonApiDocument {

  private ResourceObject data;

  public UUID getId() {
    return data != null ? data.getId() : null;
  }

  public Map<String, Object> getAttributes() {
    return data != null ? data.getAttributes() : null;
  }

  public Map<String, RelationshipObject> getRelationships() {
    return data != null ? data.getRelationships() : null;
  }

  /**
   * Defines a single-resource object as per <a href="https://jsonapi.org/format/#document-resource-objects">...</a>
   */
  @Jacksonized
  @Builder
  @Getter
  public static class ResourceObject {
    private String type;
    private UUID id;

    private Map<String, Object> attributes;

    private Map<String, RelationshipObject> relationships;

    public Set<String> getAttributesName() {
      return attributes != null ? attributes.keySet() : Set.of();
    }

    public Set<String> getRelationshipsName() {
      return relationships != null ? relationships.keySet() : Set.of();
    }
  }

  /**
   * Defines a relationship object as per <a href="https://jsonapi.org/format/#document-resource-object-relationships">...</a>
   */
  @Jacksonized
  @Builder
  @Getter
  public static class RelationshipObject {

    /**
     * Could be a ResourceIdentifier (to-one) or a List of ResourceIdentifier (to-many)
     */
    private Object data;
  }

  /**
   * Defines a resource identifier object as per <a href="https://jsonapi.org/format/#document-resource-identifier-objects">...</a>
   */
  @Jacksonized
  @Builder
  @Getter
  public static class ResourceIdentifier {
    private String type;
    private UUID id;
  }
}
