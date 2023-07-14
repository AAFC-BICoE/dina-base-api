package ca.gc.aafc.dina.jsonapi;

import java.util.HashMap;
import java.util.Map;
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
  public static Optional<JsonNode> atJsonPtr(JsonNode document, JsonPointer ptr) {
    JsonNode node = document.at(ptr);
    return node.isMissingNode() ? Optional.empty() : Optional.of(node);
  }


  /**
   * If the value of the map is another map, merge it using dot notation.
   * Currently, limited to 1 level.
   * Given:
   *   "attribute1": "val1",
   *   "attribute2": {
   *     "nested1": "val nested 1"
   *   }
   *
   * Will output:
   *   "attribute1": "val1",
   *   "attribute2.nested1": "val nested 1"
   * @param theMap
   * @return
   */
  public static Map<String, Object> mergeNestedMapUsingDotNotation(Map<String, Object> theMap) {
    Map<String, Object> newMap = new HashMap<>();
    for (var entry : theMap.entrySet()) {
      if(entry.getValue() instanceof Map<?,?> entryAsMap) {
        for (var b : entryAsMap.entrySet()) {
          newMap.put(entry.getKey() + "." + b.getKey(), b.getValue());
        }
      } else {
        // keep it as is
        newMap.put(entry.getKey(), entry.getValue());
      }
    }
    return newMap;
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
