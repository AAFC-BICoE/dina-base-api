package ca.gc.aafc.dina.jsonapi;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import ca.gc.aafc.dina.testsupport.TestResourceHelper;

import static org.junit.jupiter.api.Assertions.*;

public class JSONApiDocumentStructureTest {

  @Test
  public void onIsRelationshipsPath_expectedResultReturned() {
    assertFalse(JSONApiDocumentStructure.isRelationshipsPath(null));
    assertFalse(JSONApiDocumentStructure.isRelationshipsPath(""));
    assertFalse(JSONApiDocumentStructure.isRelationshipsPath("node.data.relationships"));

    assertTrue(JSONApiDocumentStructure.isRelationshipsPath("data.relationships.node"));
  }

  @Test
  public void onIsAttributesPath_expectedResultReturned() {
    assertFalse(JSONApiDocumentStructure.isAttributesPath(null));
    assertFalse(JSONApiDocumentStructure.isAttributesPath(""));
    assertFalse(JSONApiDocumentStructure.isAttributesPath("node.data.attributes"));

    assertTrue(JSONApiDocumentStructure.isAttributesPath("data.attributes.node"));
  }

  @Test
  public void onRemoveAttributesPrefix_expectedResultReturned() {
    assertNull(JSONApiDocumentStructure.removeAttributesPrefix(null));
    assertEquals("", JSONApiDocumentStructure.removeAttributesPrefix(""));
    assertEquals("node.data.attributes", JSONApiDocumentStructure.removeAttributesPrefix("node.data.attributes"));

    assertEquals("node", JSONApiDocumentStructure.removeAttributesPrefix("data.attributes.node"));
  }

  @Test
  public void onatJsonPtr_JsonNodeAtPointerReturned(){
    Map<String, Object> testDocument = Map.of("data", Map.of("attributes", Map.of("attribute1", "value1")));

    Optional<JsonNode> attributeNode = JSONApiDocumentStructure.atJsonPtr(TestResourceHelper.OBJECT_MAPPER.valueToTree(testDocument), JSONApiDocumentStructure.ATTRIBUTES_PTR);
    assertTrue(attributeNode.isPresent());
    assertEquals("value1", attributeNode.get().get("attribute1").asText());
  }

  @Test
  public void onMergeNestedMapUsingDotNotation_expectedResultReturned() {
    Map<String, Object> testMap = Map.of(
      "attribute1", "val1",
      "attribute2", Map.of("nested1", "val nested 1"));

    Map<String, Object> mergedMap = JSONApiDocumentStructure.mergeNestedMapUsingDotNotation(testMap);
    assertEquals("val nested 1", mergedMap.get("attribute2.nested1"));
  }
}
