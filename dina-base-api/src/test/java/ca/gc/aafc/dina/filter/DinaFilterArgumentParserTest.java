package ca.gc.aafc.dina.filter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

class DinaFilterArgumentParserTest {

  @Test
  void parse_SupportsLocalDateTime() {
    DinaFilterArgumentParser parser = new DinaFilterArgumentParser();
    LocalDateTime result = parser.parse("2007-12-03T10:15:30", LocalDateTime.class);
    Assertions.assertEquals(LocalDateTime.of(2007, 12, 3, 10, 15, 30), result);
  }
}