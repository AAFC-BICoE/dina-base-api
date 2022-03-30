package ca.gc.aafc.dina.i18n;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MultilingualDescriptionTest {

  @Test
  public void multilingualDescription_onDescription_hasBlankDescriptionReturnsRightResult() {
    MultilingualDescription multilingualDescription = MultilingualDescription.builder()
        .descriptions(List.of(
            MultilingualDescription.MultilingualPair.of("en", "en"),
            MultilingualDescription.MultilingualPair.of("fr", "")))
        .build();
    assertTrue(multilingualDescription.hasBlankDescription());

    // if the list of description is empty it should return false since there is no description to validate
    assertFalse(MultilingualDescription.builder().build().hasBlankDescription());
  }

}
