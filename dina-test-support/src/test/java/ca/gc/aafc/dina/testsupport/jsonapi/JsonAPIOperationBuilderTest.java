package ca.gc.aafc.dina.testsupport.jsonapi;

import ca.gc.aafc.dina.testsupport.entity.ComplexObject;
import io.crnk.core.engine.http.HttpMethod;
import lombok.Data;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class JsonAPIOperationBuilderTest {

  @Data
  static class TestObject extends ComplexObject {
    private String email;
    private String displayName;
  }

  private static JsonAPITestHelperTest.TestObject createTestObject() {
    JsonAPITestHelperTest.TestObject myTestObj = new JsonAPITestHelperTest.TestObject();
    myTestObj.setDisplayName("agent");
    myTestObj.setEmail("a@a.ca");
    return myTestObj;
  }

  @Test
  public void operationBuilder_onValidArgument_ValidOperationJSONCreated() {

    Map<String, Object> values = JsonAPITestHelper.toJsonAPIMap("metadata",
        JsonAPITestHelper.toAttributeMap(createTestObject()), null, null, null);

    List<Map<String, Object>> operationJsonMap = JsonAPIOperationBuilder.newBuilder()
        .addOperation(HttpMethod.POST, "agent", values)
        .addOperation(HttpMethod.POST, "agent", values)
        .buildOperation();

    assertEquals( 2, operationJsonMap.size());

    assertEquals("POST", operationJsonMap.get(0).get("op"));
    assertNotNull(operationJsonMap.get(0).get("value"));
  }

}
