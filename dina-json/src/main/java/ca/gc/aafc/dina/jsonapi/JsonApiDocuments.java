package ca.gc.aafc.dina.jsonapi;

import java.util.Map;
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
    return JsonApiDocument.builder()
      .data(
        JsonApiDocument.ResourceObject.builder()
          .id(uuid)
          .type(type)
          .attributes(attributes)
          .build()
      ).build();
  }
}
