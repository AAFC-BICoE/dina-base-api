package ca.gc.aafc.dina.filter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.UUID;

class DinaFilterArgumentParserTest {

  private enum TestEnum { ALPHA, BETA }

  private final DinaFilterArgumentParser parser = new DinaFilterArgumentParser();

  @Test
  void parse_SupportsLocalDateTime() {
    LocalDateTime result = parser.parse("2007-12-03T10:15:30", LocalDateTime.class);
    Assertions.assertEquals(LocalDateTime.of(2007, 12, 3, 10, 15, 30), result);
  }

  @Test
  void parse_NullValues_ReturnNull() {
    Assertions.assertNull(parser.parse(null, Integer.class));
    Assertions.assertNull(parser.parse("null", String.class));
    Assertions.assertNull(parser.parse(" NULL ", UUID.class));
  }

  @Test
  void parse_CommonTypes() {
    Assertions.assertEquals("abc", parser.parse("abc", String.class));
    Assertions.assertEquals(12, parser.parse("12", Integer.class));
    Assertions.assertEquals(12, parser.parse("12", int.class));
    Assertions.assertEquals(12L, parser.parse("12", Long.class));
    Assertions.assertEquals(1.5d, parser.parse("1.5", Double.class));
    Assertions.assertEquals(1.5f, parser.parse("1.5", float.class));
    Assertions.assertEquals(Boolean.TRUE, parser.parse("true", Boolean.class));
    Assertions.assertEquals(new BigDecimal("10.25"), parser.parse("10.25", BigDecimal.class));
    Assertions.assertEquals(TestEnum.BETA, parser.parse("BETA", TestEnum.class));
  }

  @Test
  void parse_DinaTypes() {
    UUID uuid = UUID.randomUUID();
    Assertions.assertEquals(uuid, parser.parse(uuid.toString(), UUID.class));
    Assertions.assertEquals(OffsetDateTime.of(2007, 12, 3, 10, 15, 30, 0, ZoneOffset.ofHours(1)),
      parser.parse("2007-12-03T10:15:30+01:00", OffsetDateTime.class));
  }

  @Test
  void parse_Date_SupportsDateTimeAndDate() {
    Date expectedDateTime = Date.from(LocalDateTime.of(2007, 12, 3, 10, 15, 30)
      .atZone(ZoneId.systemDefault()).toInstant());
    Assertions.assertEquals(expectedDateTime, parser.parse("2007-12-03T10:15:30", Date.class));

    Date expectedDate = Date.from(LocalDate.of(2007, 12, 3)
      .atStartOfDay(ZoneId.systemDefault()).toInstant());
    Assertions.assertEquals(expectedDate, parser.parse("2007-12-03", Date.class));
  }

  @Test
  void parse_FallbackOnValueOf() {
    Assertions.assertEquals((short) 3, parser.parse("3", Short.class));
  }

  @Test
  void parse_InvalidArgument_ThrowsIllegalArgumentException() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> parser.parse("abc", Integer.class));
    Assertions.assertThrows(IllegalArgumentException.class, () -> parser.parse("abc", UUID.class));
    Assertions.assertThrows(IllegalArgumentException.class, () -> parser.parse("abc", OffsetDateTime.class));
    Assertions.assertThrows(IllegalArgumentException.class, () -> parser.parse("GAMMA", TestEnum.class));
    Assertions.assertThrows(IllegalArgumentException.class, () -> parser.parse("abc", Date.class));
    Assertions.assertThrows(IllegalArgumentException.class, () -> parser.parse("abc", Short.class));
  }

  @Test
  void parse_UnsupportedType_ThrowsIllegalArgumentException() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> parser.parse("abc", Object.class));
  }
}
