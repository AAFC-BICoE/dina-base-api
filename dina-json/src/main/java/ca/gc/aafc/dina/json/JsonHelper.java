package ca.gc.aafc.dina.json;

import java.util.Objects;
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
   * Safely compares the text value of a field in a JSON node with an expected value.
   * Returns false if the node is null, the field doesn't exist, the field is null, or the values don't match.
   *
   * @param node The JSON node to check.
   * @param fieldName The name of the field to check.
   * @param expectedValue The expected text value.
   * @return True if the field exists and its text value matches the expected value, false otherwise.
   */
  public static boolean safeTextEquals(JsonNode node, String fieldName, String expectedValue) {
    Objects.requireNonNull(node, "node must not be null");
    Objects.requireNonNull(fieldName, "fieldName must not be null");
    Objects.requireNonNull(expectedValue, "expectedValue must not be null");
    
    JsonNode fieldNode = node.get(fieldName);
    // Return false if field doesn't exist or is JSON null
    if (fieldNode == null || fieldNode.isNull()) {
      return false;
    }
    
    return fieldNode.asText().equals(expectedValue);
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
