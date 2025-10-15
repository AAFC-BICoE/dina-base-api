package ca.gc.aafc.dina.datetime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ISODateTimeTest {

  @ParameterizedTest @ValueSource(strings = { "2019", "2019-08", "2019-08-23",
      "2019-08-23T12:24:34", "2019-08-23T12:24", "2020-12-23T05:01:02.333333", "2020-12-23T05:01:02.333" })
  public void testRoundTrip(String input) {
    assertEquals(input, ISODateTime.parse(input).toString());
  }

  @Test
  public void testMilliPrecisionBelow3RoundTrip() {
    assertEquals("2020-12-23T05:01:02.300", ISODateTime.parse("2020-12-23T05:01:02.3").toString());
    assertEquals("2020-12-23T05:01:02.330", ISODateTime.parse("2020-12-23T05:01:02.33").toString());
  }

  @ParameterizedTest @ValueSource(strings = { "abc", "2019-45", "2019-08-32", "2019-08-23T12:24:75" })
  public void testWrongDate(String input) {
    assertThrows(DateTimeParseException.class, () -> ISODateTime.parse(input));
  }

  @Test
  void parse_YYYY_shouldCreateCorrectRange() {
    ISODateTime result = ISODateTime.parse("2004");
    assertEquals(LocalDateTime.of(2004, 1, 1, 0, 0, 0, 0),
      result.getLocalDateTime());
    assertEquals(LocalDateTime.of(2004, 12, 31, 23, 59, 59, 999_000_000),
      result.getLocalEndDateTime());
    assertEquals(ISODateTime.Format.YYYY, result.getFormat());
  }

  @Test
  void parse_YYYY_MM_shouldCreateCorrectRange() {
    ISODateTime result = ISODateTime.parse("2004-06");
    assertEquals(LocalDateTime.of(2004, 6, 1, 0, 0, 0, 0),
      result.getLocalDateTime());
    assertEquals(LocalDateTime.of(2004, 6, 30, 23, 59, 59, 999_000_000),
      result.getLocalEndDateTime());
    assertEquals(ISODateTime.Format.YYYY_MM, result.getFormat());
  }

  @Test
  void parse_YYYY_MM_DD_shouldCreateCorrectRange() {
    ISODateTime result = ISODateTime.parse("2004-06-15");

    assertEquals(LocalDateTime.of(2004, 6, 15, 0, 0, 0, 0),
      result.getLocalDateTime());
    assertEquals(LocalDateTime.of(2004, 6, 15, 23, 59, 59, 999_000_000),
      result.getLocalEndDateTime());
    assertEquals(ISODateTime.Format.YYYY_MM_DD, result.getFormat());
  }

  @Test
  void parse_YYYY_MM_DD_HH_MM_shouldCreateCorrectRange() {
    ISODateTime result = ISODateTime.parse("2004-06-15T14:30");

    assertEquals(LocalDateTime.of(2004, 6, 15, 14, 30, 0, 0),
      result.getLocalDateTime());
    assertEquals(LocalDateTime.of(2004, 6, 15, 14, 30, 59, 999_000_000),
      result.getLocalEndDateTime());
    assertEquals(ISODateTime.Format.YYYY_MM_DD_HH_MM, result.getFormat());
  }

  @Test
  void parse_YYYY_MM_DD_HH_MM_SS_shouldCreateCorrectRange() {
    ISODateTime result = ISODateTime.parse("2004-06-15T14:30:45");

    assertEquals(LocalDateTime.of(2004, 6, 15, 14, 30, 45, 0),
      result.getLocalDateTime());
    assertEquals(LocalDateTime.of(2004, 6, 15, 14, 30, 45, 999_000_000),
      result.getLocalEndDateTime());
    assertEquals(ISODateTime.Format.YYYY_MM_DD_HH_MM_SS, result.getFormat());
  }

  @Test
  void parse_YYYY_MM_DD_HH_MM_SS_MMM_shouldHaveNoAmbiguity() {
    ISODateTime result = ISODateTime.parse("2004-06-15T14:30:45.123");

    assertEquals(LocalDateTime.of(2004, 6, 15, 14, 30, 45, 123_000_000),
      result.getLocalDateTime());
    assertEquals(LocalDateTime.of(2004, 6, 15, 14, 30, 45, 123_000_000),
      result.getLocalEndDateTime());
    assertEquals(ISODateTime.Format.YYYY_MM_DD_HH_MM_SS_MMM, result.getFormat());
  }

  @Test
  void parse_YYYY_MM_DD_HH_MM_SS_withMorePrecision_shouldHaveNoAmbiguity() {
    ISODateTime result = ISODateTime.parse("2004-06-15T14:30:45.123456789");

    LocalDateTime expected = LocalDateTime.of(2004, 6, 15, 14, 30, 45, 123_456_789);
    assertEquals(expected, result.getLocalDateTime());
    assertEquals(expected, result.getLocalEndDateTime());
    assertEquals(ISODateTime.Format.YYYY_MM_DD_HH_MM_SS_MMM, result.getFormat());
  }
}
