package ca.gc.aafc.dina.testsupport;

import tools.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestResourceHelperTest {

  @Test
  void readJsonResource_onValidResource_ValidJsonNode() throws IOException {
    JsonNode node = TestResourceHelper.readContentAsJsonNode("missingAttribute.json");
    assertEquals("data", node.propertyNames().iterator().next());
  }
}
