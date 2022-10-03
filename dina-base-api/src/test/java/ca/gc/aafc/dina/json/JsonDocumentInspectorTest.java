package ca.gc.aafc.dina.json;

import ca.gc.aafc.dina.i18n.MultilingualDescription;
import ca.gc.aafc.dina.testsupport.jsonapi.JsonAPITestHelper;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class JsonDocumentInspectorTest {

  @Test
  public void testPredicateOnValues_onPredicateReturnsFalse_inspectorReturnsFalse() {

    MultilingualDescription multilingualDescription = MultilingualDescription.builder()
            .descriptions(List.of(
                    MultilingualDescription.MultilingualPair.of("en", "en"),
                    MultilingualDescription.MultilingualPair.of("fr", "")))
            .build();

    assertFalse(JsonDocumentInspector.testPredicateOnValues(
            JsonAPITestHelper.toAttributeMap(multilingualDescription), StringUtils::isNotBlank));
  }
}
