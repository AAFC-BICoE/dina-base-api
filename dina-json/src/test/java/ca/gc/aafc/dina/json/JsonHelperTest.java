package ca.gc.aafc.dina.json;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.jsonpath.TypeRef;

import ca.gc.aafc.dina.jsonapi.JSONApiDocumentStructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JsonHelperTest {

  private static final Resource resource = new ClassPathResource("jsonhelper-test-data.json");

  @Test
  public void onUtilityFunctions_expectedResultReturned(){
    Map<String, Object> testDocument = Map.of("data", Map.of("attributes",
      Map.of(
        "attribute1", "value1",
        "attribute2", Map.of("attribute2_1", "value2_1"),
        "attribute3", List.of("value3_1", "value3_2"))));

    Optional<JsonNode> attributeNode = JsonHelper.atJsonPtr(TestConstants.OBJECT_MAPPER.valueToTree(testDocument), JSONApiDocumentStructure.ATTRIBUTES_PTR);
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

    // safeTextEquals
    assertTrue(JsonHelper.safeTextEquals(attributeNode.get(), "attribute1", "value1"));
    assertFalse(JsonHelper.safeTextEquals(attributeNode.get(), "attribute1", "wrongValue"));
    assertFalse(JsonHelper.safeTextEquals(attributeNode.get(), "attributeXYZ", "value1"));
  }

  @Test
  public void testFindActiveSpecimen() throws IOException {
    JsonNode node = TestConstants.OBJECT_MAPPER.readTree(resource.getInputStream());
    JsonNode result = JsonHelper.findOneInJsonNode(node,
      "$.data[?(@.attributes.isActive == true)]");

    assertNotNull(result);
    assertEquals("spec-002", result.get("id").asText());
    assertTrue(result.get("attributes").get("isActive").asBoolean());
  }

  @Test
  public void testGetAllSpecimenIds() throws IOException {
    JsonNode jsonNode = TestConstants.OBJECT_MAPPER.readTree(resource.getInputStream());
    List<JsonNode> results = JsonHelper.findAllInJsonNode(jsonNode,
      "$.data[*].id");

    assertNotNull(results);
    assertEquals(3, results.size());
    assertEquals("spec-001", results.get(0).asText());
    assertEquals("spec-002", results.get(1).asText());
    assertEquals("spec-003", results.get(2).asText());
  }

  @Test
  public void testFindOneInJsonNode_definitePath() throws IOException {
    JsonNode node = TestConstants.OBJECT_MAPPER.readTree(resource.getInputStream());
    JsonNode result = JsonHelper.findOneInJsonNode(node, "$.data");

    assertNotNull(result);
    assertTrue(result.isArray());
    assertEquals(3, result.size());
  }
}
