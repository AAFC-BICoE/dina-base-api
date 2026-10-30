package ca.gc.aafc.dina.jpa;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import ca.gc.aafc.dina.i18n.MultilingualDescription;
import ca.gc.aafc.dina.i18n.MultilingualTitle;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DinaJackson3JsonFormatMapperTest {

  private final DinaJackson3JsonFormatMapper mapper = new DinaJackson3JsonFormatMapper();

  @Test
  public void toString_onDates_sameFormatAsPreviousMapper() {
    assertEquals("[2024,5,1]", mapper.toString(LocalDate.of(2024, 5, 1), LocalDate.class));
    assertEquals("\"2024-05-01T10:30:00Z\"",
      mapper.toString(OffsetDateTime.of(2024, 5, 1, 10, 30, 0, 0, ZoneOffset.UTC), OffsetDateTime.class));
  }

  @Test
  public void fromString_onMultilingualValues_copiesAreEqual() {
    String title = "{\"titles\":[{\"lang\":\"en\",\"title\":\"Title\"}]}";
    String desc = "{\"descriptions\":[{\"lang\":\"en\",\"desc\":\"Description\"}]}";

    // Hibernate uses equals to detect changes on JSON columns
    assertEquals((MultilingualTitle) mapper.fromString(title, MultilingualTitle.class),
      mapper.fromString(title, MultilingualTitle.class));
    assertEquals((MultilingualDescription) mapper.fromString(desc, MultilingualDescription.class),
      mapper.fromString(desc, MultilingualDescription.class));
  }
}
