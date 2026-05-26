package ca.gc.aafc.dina.json;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.ParseContext;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.TypeRef;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Utility methods to work with Jackson's Json objects.
 */
public final class JsonHelper {

  private static final ParseContext PARSE_CONTEXT = JsonPath.using(
    Configuration.builder()
      .jsonProvider(new JacksonJsonNodeJsonProvider())
      .mappingProvider(new JacksonMappingProvider())
      .options(Option.ALWAYS_RETURN_LIST)
      .build());

  private static final TypeRef<List<JsonNode>> JSON_NODE_TYPEREF = new TypeRef<>() {
  };

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

  /**
   * Find first matching element in JsonNode
   * @param node the json node
   * @param jsonPathExpression the JsonPath expression
   * @return the first matching JsonNode, or null if not found
   * @see <a href="https://github.com/json-path/JsonPath">JsonPath GitHub Repository</a>
   */
  public static JsonNode findOneInJsonNode(JsonNode node, String jsonPathExpression) {
    List<JsonNode> result = findInJsonNode(node, jsonPathExpression, JSON_NODE_TYPEREF);
    return result != null && !result.isEmpty() ? result.getFirst() : null;
  }


  /**
   * Find all matching elements in JsonNode
   * @param node the json node
   * @param jsonPathExpression the JsonPath expression
   * @return List of matching JsonNodes, or empty list if not found
   * @see <a href="https://github.com/json-path/JsonPath">JsonPath GitHub Repository</a>
   */
  public static List<JsonNode> findAllInJsonNode(JsonNode node, String jsonPathExpression) {
    List<JsonNode> result = findInJsonNode(node, jsonPathExpression, JSON_NODE_TYPEREF);
    return result != null ? result : List.of();
  }

  /**
   * Generic method to find element in JsonNode
   * @param node the json node
   * @param jsonPathExpression the JsonPath expression
   * @param typeRef the TypeRef specifying the return type
   * @return the result of the specified type, or null if not found
   */
  public static <T> T findInJsonNode(JsonNode node, String jsonPathExpression,
                                     TypeRef<T> typeRef) {
    try {
      DocumentContext dc = PARSE_CONTEXT.parse(node);
      return dc.read(jsonPathExpression, typeRef);
    } catch (PathNotFoundException pnf) {
      return null;
    }
  }
}
