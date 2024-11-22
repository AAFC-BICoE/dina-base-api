package ca.gc.aafc.dina.json;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JsonDocumentInspectorTest {

  @Test
  public void testPredicateOnValues_onPredicateReturnsFalse_inspectorReturnsFalse()
    throws JsonProcessingException {

    Map<String, Object> multilingualDescription = TestConstants.OBJECT_MAPPER.readValue(
      """
        {"descriptions":[{"lang":"en","desc":"en"},{"lang":"fr","desc":""}]}
        """, TestConstants.IT_OM_TYPE_REF);

    assertFalse(JsonDocumentInspector.testPredicateOnValues(
      multilingualDescription, StringUtils::isNotBlank));
  }

  @Test
  public void testPredicateOnValues_onPredicateReturnsFalse_inspectorReturnsFalseOnArrayElements()
    throws JsonProcessingException {

    Map<String, Object> names = TestConstants.OBJECT_MAPPER.readValue(
      """
        {"names":["name 1", ""]}}
        """, TestConstants.IT_OM_TYPE_REF);
    assertFalse(JsonDocumentInspector.testPredicateOnValues(
      names, StringUtils::isNotBlank));

    // make sure our assumption is right
    names = TestConstants.OBJECT_MAPPER.readValue(
      """
        {"names":["name 1", "name 2"]}}
        """, TestConstants.IT_OM_TYPE_REF);
    assertTrue(JsonDocumentInspector.testPredicateOnValues(
      names, StringUtils::isNotBlank));
  }

  @Test
  public void testPredicateOnValues_onPredicateReturnsFalse_inspectorReturnsFalseOnNestedArrayElements()
    throws JsonProcessingException {
    Map<String, Object> multilingualDescription = TestConstants.OBJECT_MAPPER.readValue(
      """
        {"descriptions":[{"lang":"en","desc":"en"},{"lang":"fr","desc":"fr", "notes":["a", ""]}]}
        """, TestConstants.IT_OM_TYPE_REF);

    assertFalse(JsonDocumentInspector.testPredicateOnValues(
      multilingualDescription, StringUtils::isNotBlank));
  }
}
