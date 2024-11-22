package ca.gc.aafc.dina.jsonapi;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
    if (StringUtils.isBlank(currentPath)) {
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
    if (StringUtils.isBlank(currentPath)) {
      return false;
    }
    return currentPath.startsWith(DATA_ATTRIBUTES_PATH);
  }

  /**
   * @deprecated use JsonHelper
   * Returns a document's part represented by the JsonPointer or Optional.empty if not found.
   * @param document
   * @param ptr
   * @return
   */
  @Deprecated(forRemoval = true)
  public static Optional<JsonNode> atJsonPtr(JsonNode document, JsonPointer ptr) {
    JsonNode node = document.at(ptr);
    return node.isMissingNode() ? Optional.empty() : Optional.of(node);
  }
  
  /**
   * If the value of a map entry from the provided map is another map, merge it using dot notation.
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
      if (entry.getValue() instanceof Map<?,?> entryAsMap) {
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
   * If the value of a map entry from the provided map is another map, extract it using dot notation.
   * This function will recursively extract nested maps.
   * See {@link #mergeNestedMapUsingDotNotation(Map)}
   * @param theMap
   * @return result of the extraction as {@link ExtractNestedMapResult}
   */
  public static ExtractNestedMapResult extractNestedMapUsingDotNotation(Map<String, Object> theMap) {
    Set<String> keysUsed = new HashSet<>();
    Map<String, Object> newMap = new HashMap<>();

    for (var entry : theMap.entrySet()) {
      if (entry.getValue() instanceof Map<?, ?> entryAsMapInitialLevel) {
        keysUsed.add(entry.getKey());
        // Try to see if we have another map inside the current map
        ExtractNestedMapResult nextLevelResult = extractNestedMapUsingDotNotation(
          (Map<String, Object>) entryAsMapInitialLevel);

        // add elements under the new computed key except if the value is another map
        for (var initialMapElement : entryAsMapInitialLevel.entrySet()) {
          if (!nextLevelResult.isUsedKey(initialMapElement.getKey().toString())) {
            addToMapWithContext(entry.getKey(), initialMapElement.getKey().toString(), initialMapElement.getValue(), newMap);
          }
        }
        // add the elements of the next (deeper) map (if there are some)
        for (var nextLevelMapElement : nextLevelResult.nestedMapsMap().entrySet()) {
          addToMapWithContext(entry.getKey(),
            nextLevelMapElement.getKey(), nextLevelMapElement.getValue(), newMap);
        }
      }
    }
    return new ExtractNestedMapResult(newMap, keysUsed);
  }

  /**
   * Compute a new key with a context prefix to add a value to the provided map.
   * @param context
   * @param key
   * @param value
   * @param theMap
   */
  private static void addToMapWithContext(String context, String key, Object value, Map<String, Object> theMap) {
    theMap.put(context + "." + key, value);
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

  /**
   * Represents the extraction of nested maps.
   * @param nestedMapsMap the map containing the nested maps where the key is using the dot notation
   * @param usedKeys keys that were representing a map before getting extracted into nestedMapsMap
   */
  public record ExtractNestedMapResult(Map<String, Object> nestedMapsMap, Set<String> usedKeys) {

    public boolean isUsedKey(String key) {
      if (usedKeys == null || key == null) {
        return false;
      }
      return usedKeys.contains(key);
    }
  }

}
