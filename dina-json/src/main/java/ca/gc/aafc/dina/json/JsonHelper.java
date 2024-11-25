package ca.gc.aafc.dina.json;

import java.util.Optional;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Utility methods to work with Jackson's Json objects.
 */
public final class JsonHelper {

  private JsonHelper() {
    // utility class
  }

  public static Optional<JsonNode> atJsonPtr(JsonNode document, JsonPointer ptr) {
    JsonNode node = document.at(ptr);
    return node.isMissingNode() ? Optional.empty() : Optional.of(node);
  }

  /**
   * Checks if a JSON node has a specific field and if that field's value is an array.
   *
   * @param node The JSON node to check.
   * @param fieldName The name of the field to check.
   * @return True if the node has the field and its value is an array, false otherwise.
   */
  public static boolean hasFieldAndIsArray(JsonNode node, String fieldName) {
    return node.has(fieldName) && node.get(fieldName).isArray();
  }

  /**
   * Checks if a JSON node has a specific field and if that field's value is an object.
   *
   * @param node The JSON node to check.
   * @param fieldName The name of the field to check.
   * @return True if the node has the field and its value is an object, false otherwise.
   */
  public static boolean hasFieldAndIsObject(JsonNode node, String fieldName) {
    return node.has(fieldName) && node.get(fieldName).isObject();
  }

  /**
   * Returns the content of the field as text and return empty string if the field is not present.
   *
   * @param objNode
   * @param fieldName
   * @return
   */
  public static String safeAsText(JsonNode objNode, String fieldName) {
    return objNode.has(fieldName) ? objNode.get(fieldName).asText() : "";
  }
}
