package ca.gc.aafc.dina.jsonapi;

import java.util.Optional;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;

import org.apache.commons.lang3.StringUtils;

/**
 * Collections of constants and utilities for JSON:API related code.
 */
public final class JSONApiDocumentStructure {

  // Utility class
  private JSONApiDocumentStructure() {
  }

  public static final String DATA = "data";
  public static final String INCLUDED = "included";
  public static final String META = "meta";

  public static final String ATTRIBUTES = "attributes";
  public static final String RELATIONSHIPS = "relationships";
  public static final String ID = "id";
  public static final String TYPE = "type";

  // path using dot notation (e.g. data.attributes)
  public static final String DATA_ATTRIBUTES_PATH = DATA + "." + ATTRIBUTES;
  public static final String DATA_RELATIONSHIPS_PATH = DATA + "." + RELATIONSHIPS;

  // JSON Pointer version
  public static final JsonPointer DATA_PTR = JsonPointer.valueOf("/" + DATA);
  public static final JsonPointer INCLUDED_PTR = JsonPointer.valueOf("/" + INCLUDED);
  public static final JsonPointer META_PTR = JsonPointer.valueOf("/" + META);
  public static final JsonPointer ATTRIBUTES_PTR = JsonPointer.valueOf("/" + DATA + "/" + ATTRIBUTES);
  public static final JsonPointer RELATIONSHIP_PTR = JsonPointer.valueOf("/" + DATA + "/" + RELATIONSHIPS);


  /**
   * Checks if the provided path refers to a relationships' path (data.relationships).
   * @param currentPath path using dot notation
   * @return
   */
  public static boolean isRelationshipsPath(String currentPath) {
    if(StringUtils.isBlank(currentPath)) {
      return false;
    }
    return currentPath.startsWith(DATA_RELATIONSHIPS_PATH);
  }

  /**
   * Checks if the provided path refers to an attribute' path (data.attributes).
   * @param currentPath path using dot notation
   * @return
   */
  public static boolean isAttributesPath(String currentPath) {
    if(StringUtils.isBlank(currentPath)) {
      return false;
    }
    return currentPath.startsWith(DATA_ATTRIBUTES_PATH);
  }

  /**
   * Returns a document's part represented by the JsonPointer or Optional.empty if not found.
   * @param document
   * @param ptr
   * @return
   */
  private static Optional<JsonNode> atJsonPtr(JsonNode document, JsonPointer ptr) {
    JsonNode node = document.at(ptr);
    return node.isMissingNode() ? Optional.empty() : Optional.of(node);
  }

  /**
   * Removes the attributes prefix from the current path.
   * "data.attributes.name" -> "name"
   * @param currentPath
   * @return the current path without the data.attributes. prefix or the provided string is the prefix is not present
   */
  public static String removeAttributesPrefix(String currentPath) {
    return StringUtils.removeStart(
            StringUtils.removeStart(currentPath, DATA_ATTRIBUTES_PATH), ".");
  }

}
