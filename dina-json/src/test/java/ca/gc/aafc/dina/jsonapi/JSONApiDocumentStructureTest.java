package ca.gc.aafc.dina.jsonapi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

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
    // attributes1 should be there
    assertEquals("val1", mergedMap.get("attribute1"));
  }

  @Test
  public void onExtractNestedMapUsingDotNotation_expectedResultReturned() {

    Map<String, Object> testMap = Map.of(
      "attribute1", "val1",
      "attribute2", Map.of("nested1", "val nested 1"),
      "attribute3", Map.of("nestedMap",
        Map.of("nested3_1", "val nested 3_1", "nested3_2", "val nested 3_2", "nested_3_3", Map.of("nested_3_3_1", "val nested 3_3_1"))));

    JSONApiDocumentStructure.ExtractNestedMapResult nestedMap =
      JSONApiDocumentStructure.extractNestedMapUsingDotNotation(testMap);

    assertTrue(nestedMap.usedKeys().contains("attribute2"));
    assertTrue(nestedMap.usedKeys().contains("attribute3"));
    assertEquals("val nested 1", nestedMap.nestedMapsMap().get("attribute2.nested1"));
    assertEquals("val nested 3_2", nestedMap.nestedMapsMap().get("attribute3.nestedMap.nested3_2"));
    assertEquals("val nested 3_3_1", nestedMap.nestedMapsMap().get("attribute3.nestedMap.nested_3_3.nested_3_3_1"));

    // attribute1 should not be extracted since it doesn't point to a map
    assertNull(nestedMap.nestedMapsMap().get("attribute1"));
  }

}
