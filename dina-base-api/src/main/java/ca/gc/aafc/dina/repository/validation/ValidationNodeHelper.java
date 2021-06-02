package ca.gc.aafc.dina.repository.validation;

import com.fasterxml.jackson.databind.JsonNode;

public final class ValidationNodeHelper {

  public static final String ATTRIBUTES_KEY = "attributes";
  public static final String RELATIONSHIPS_KEY = "relationships";

  private ValidationNodeHelper() {
  }

  public static boolean isBlank(JsonNode data) {
    return data == null || data.isNull() || data.isEmpty();
  }

  public static boolean isInvalidDataBlock(JsonNode data) {
    return isBlank(data) || !data.has(ATTRIBUTES_KEY) || isBlank(data.get(ATTRIBUTES_KEY));
  }

}
