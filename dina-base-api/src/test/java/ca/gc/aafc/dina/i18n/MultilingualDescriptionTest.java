package ca.gc.aafc.dina.i18n;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class MultilingualDescriptionTest {

  @Test
  public void multilingualDescription_onBlankDescription_hasBlankDescriptionReturnsTrue() {
    MultilingualDescription multilingualDescription = MultilingualDescription.builder()
        .descriptions(List.of(
            MultilingualDescription.MultilingualPair.of("en", "en"),
            MultilingualDescription.MultilingualPair.of("fr", "")))
        .build();
    assertTrue(multilingualDescription.hasBlankDescription());
  }

}
