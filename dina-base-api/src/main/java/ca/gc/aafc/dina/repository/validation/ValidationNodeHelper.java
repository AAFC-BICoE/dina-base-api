package ca.gc.aafc.dina.repository.validation;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Utility class to help with validation of Json Nodes.
 */
public final class ValidationNodeHelper {

  public static final String ATTRIBUTES_KEY = "attributes";
  public static final String RELATIONSHIPS_KEY = "relationships";

  private ValidationNodeHelper() {
  }

  /**
   * Returns true if the given json node is null or empty.
   *
   * @param data node to evaluate
   * @return true if the given json node is null or empty.
   */
  public static boolean isBlank(JsonNode data) {
    return data == null || data.isNull() || data.isEmpty();
  }

  /**
   * Returns true if the given json node is node is empty or does not contain a attributes block.
   *
   * @param data node to evaluate
   * @return true if the given json node is node is empty or does not contain a attributes block.
   */
  public static boolean isInvalidDataBlock(JsonNode data) {
    return isBlank(data) || !data.has(ATTRIBUTES_KEY) || isBlank(data.get(ATTRIBUTES_KEY));
  }

}
