package ca.gc.aafc.dina.jsonapi;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Utility classes to simplify usage of {@link JsonApiDocument} based documents.
 */
public final class JsonApiDocuments {

  private JsonApiDocuments() {
    // utility class
  }

  /**
   * Creates a {@link JsonApiDocument} for provided parameters
   * @param uuid
   * @param type
   * @param attributes
   * @return
   */
  public static JsonApiDocument createJsonApiDocument(UUID uuid, String type,
                                                      Map<String, Object> attributes) {
    Objects.requireNonNull(uuid);
    Objects.requireNonNull(type);
    
    return JsonApiDocument.builder()
      .data(
        JsonApiDocument.ResourceObject.builder()
          .id(uuid)
          .type(type)
          .attributes(attributes)
          .build()
      ).build();
  }

  /**
   * Creates a {@link JsonApiDocument} with relationships for provided parameters
   * @param uuid
   * @param type
   * @param attributes
   * @param relationships
   * @return
   */
  public static JsonApiDocument createJsonApiDocument(UUID uuid, String type,
                                                      Map<String, Object> attributes,
                                                      Map<String, Object> relationships) {
    Objects.requireNonNull(uuid);
    Objects.requireNonNull(type);
    Objects.requireNonNull(relationships);

    var resourceObjectBuilder = JsonApiDocument.ResourceObject.builder()
      .id(uuid)
      .type(type)
      .attributes(attributes);

    if (!relationships.isEmpty()) {
      Map<String, JsonApiDocument.RelationshipObject> relationshipObjects =
        new HashMap<>(relationships.size());
      for (var rel : relationships.entrySet()) {
        relationshipObjects.put(rel.getKey(),
          JsonApiDocument.RelationshipObject.builder().data(rel.getValue()).build());
      }
      resourceObjectBuilder.relationships(relationshipObjects);
    }

    return JsonApiDocument.builder()
      .data(resourceObjectBuilder.build())
      .build();
  }
}
