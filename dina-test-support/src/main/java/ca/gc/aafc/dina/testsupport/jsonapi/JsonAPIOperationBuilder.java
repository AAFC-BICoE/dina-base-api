package ca.gc.aafc.dina.testsupport.jsonapi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpMethod;

/**
 * Helper builder to create CRNK Operation map.
 */
public final class JsonAPIOperationBuilder {

  private final List<Map<String, Object>> operations = new ArrayList<>();

  private JsonAPIOperationBuilder() {
  }

  public static JsonAPIOperationBuilder newBuilder() {
    return new JsonAPIOperationBuilder();
  }

  /**
   * Add an operation to the current builder instance.
   * @param method
   * @param path
   * @param values usually comes from JsonAPITestHelper.toJsonAPIMap
   * @return
   */
  public JsonAPIOperationBuilder addOperation(HttpMethod method, String path, Map<String, Object> values) {
    Map<String, Object> operationsMap = Map.of(
      "op", method.name(),
      "path", path,
      // since toJsonAPIMap returns the value under "data" we remove it if present
      "value", values.getOrDefault("data", values)
    );

    operations.add(operationsMap);

    return this;
  }

  public List<Map<String, Object>> buildOperation() {
    return Collections.unmodifiableList(operations);
  }

}
