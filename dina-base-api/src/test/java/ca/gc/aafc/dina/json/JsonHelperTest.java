package ca.gc.aafc.dina.json;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import ca.gc.aafc.dina.jsonapi.JSONApiDocumentStructure;
import ca.gc.aafc.dina.testsupport.TestResourceHelper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JsonHelperTest {

  @Test
  public void onUtilityFunctions_expectedResultReturned(){
    Map<String, Object> testDocument = Map.of("data", Map.of("attributes",
      Map.of(
        "attribute1", "value1",
        "attribute2", Map.of("attribute2_1", "value2_1"),
        "attribute3", List.of("value3_1", "value3_2"))));

    Optional<JsonNode> attributeNode = JsonHelper.atJsonPtr(TestResourceHelper.OBJECT_MAPPER.valueToTree(testDocument), JSONApiDocumentStructure.ATTRIBUTES_PTR);
    assertTrue(attributeNode.isPresent());
    assertEquals("value1", attributeNode.get().get("attribute1").asText());

    // safeAsText
    assertEquals("value1", JsonHelper.safeAsText(attributeNode.get(), "attribute1"));
    assertEquals("", JsonHelper.safeAsText(attributeNode.get(), "attributeXYZ"));

    // hasFieldAndIsObject
    assertTrue(JsonHelper.hasFieldAndIsObject(attributeNode.get(), "attribute2"));
    assertFalse(JsonHelper.hasFieldAndIsObject(attributeNode.get(), "attributeXYZ"));
    assertFalse(JsonHelper.hasFieldAndIsObject(attributeNode.get(), "attribute1"));

    // hasFieldAndIsArray
    assertTrue(JsonHelper.hasFieldAndIsArray(attributeNode.get(), "attribute3"));
    assertFalse(JsonHelper.hasFieldAndIsArray(attributeNode.get(), "attributeXYZ"));
    assertFalse(JsonHelper.hasFieldAndIsArray(attributeNode.get(), "attribute1"));
  }
}
