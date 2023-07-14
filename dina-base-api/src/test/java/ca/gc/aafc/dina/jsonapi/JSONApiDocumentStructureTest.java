package ca.gc.aafc.dina.jsonapi;

import java.util.Map;

import org.junit.jupiter.api.Test;

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
  public void onMergeNestedMapUsingDotNotation_expectedResultReturned() {
    Map<String, Object> testMap = Map.of(
      "attribute1", "val1",
      "attribute2", Map.of("nested1", "val nested 1"));

    Map<String, Object> mergedMap = JSONApiDocumentStructure.mergeNestedMapUsingDotNotation(testMap);
    assertEquals("val nested 1", mergedMap.get("attribute2.nested1"));
  }
}
