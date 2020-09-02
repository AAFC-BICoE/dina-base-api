package ca.gc.aafc.dina.testsupport.jsonapi;

import com.google.common.collect.ImmutableMap;
import io.crnk.core.engine.http.HttpMethod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Helper builder to create CRNK Operation map.
 */
public class JsonAPIOperationBuilder {

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

    ImmutableMap.Builder<String, Object> operationsMap = new ImmutableMap.Builder<>();
    operationsMap.put("op", method.name());
    operationsMap.put("path", path);
    // since toJsonAPIMap returns the value under "data" we remove it if present
    operationsMap.put("value", values.getOrDefault("data", values));

    operations.add(operationsMap.build());

    return this;
  }

  public List<Map<String, Object>> buildOperation() {
    return Collections.unmodifiableList(operations);
  }

}
