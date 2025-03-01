package ca.gc.aafc.dina.jsonapi;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

import ca.gc.aafc.dina.json.TestConstants;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for {@link JsonApiDocument}, {@link JsonApiCompoundDocument} and
 * {@link JsonApiDocuments}
 */
public class JsonApiDocumentTest {

  private static final String TEST_DOC = """
      {
        "data": {
          "type": "article",
          "id": "0194d686-7ac0-72d0-be8a-5e3e40e5e370",
          "attributes": {
            "title": "Rails is Omakase"
          },
          "relationships": {
            "author": {
              "links": {
                "self": "/article/1/relationships/author",
                "related": "/article/1/author"
              },
              "data": {
                "type": "people",
                "id": "0194d686-a3d1-7a41-8441-a99f171bd8f9"
              }
            }
          }
        }
      }
    """;

  private static final String TEST_COMPOUND_DOC = """
      {
        "data": {
          "type": "article",
          "id": "0194d686-7ac0-72d0-be8a-5e3e40e5e370",
          "attributes": {
            "title": "Rails is Omakase"
          },
          "relationships": {
            "author": {
              "links": {
                "self": "/article/1/relationships/author",
                "related": "/article/1/author"
              },
              "data": {
                "type": "people",
                "id": "0194d686-a3d1-7a41-8441-a99f171bd8f9"
              }
            }
          }
        },
        "included": [{
            "type": "people",
            "id": "0194d686-a3d1-7a41-8441-a99f171bd8f9",
            "attributes": {
              "firstName": "Dan",
              "lastName": "Gebhardt",
              "twitter": "dgeb"
            }
          }
        ]
      }
    """;

  @Test
  public void testParseJsonApiDocument() throws JsonProcessingException {
    JsonApiDocument jsonApiDocument = TestConstants.OBJECT_MAPPER.readValue(TEST_DOC, JsonApiDocument.class);
    JsonApiDocument JsonApiDocumentToCompare = JsonApiDocuments.createJsonApiDocument(
      UUID.fromString("0194d686-7ac0-72d0-be8a-5e3e40e5e370"), "article", Map.of());

    assertEquals(JsonApiDocumentToCompare.getId(), jsonApiDocument.getId());
    assertEquals(JsonApiDocumentToCompare.getType(), jsonApiDocument.getType());
  }

  @Test
  public void testParseJsonApiCompoundDocument() throws JsonProcessingException {
    JsonApiCompoundDocument jsonApiDocument = TestConstants.OBJECT_MAPPER.readValue(TEST_COMPOUND_DOC, JsonApiCompoundDocument.class);
    assertEquals("0194d686-7ac0-72d0-be8a-5e3e40e5e370", jsonApiDocument.getIdAsStr());
    assertEquals(1, jsonApiDocument.getIncluded().size());
  }

}
