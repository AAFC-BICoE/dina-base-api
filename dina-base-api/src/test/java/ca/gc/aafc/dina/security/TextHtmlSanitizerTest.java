package ca.gc.aafc.dina.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test related to {@link TextHtmlSanitizer}
 */
public class TextHtmlSanitizerTest {

  @Test
  public void testSafeText() {
    assertTrue(TextHtmlSanitizer.isSafeText("abc"));
    assertTrue(TextHtmlSanitizer.isSafeText("{\"id\": \"5ccf0540-27d9-4a42-b6fc-96a42d6d00f9\"}"));
  }

  @Test
  public void testUnsafeText() {
    assertFalse(TextHtmlSanitizer.isSafeText("abc<iframe src=javascript:alert(32311)>"));
    assertFalse(TextHtmlSanitizer.isSafeText("abc<iframe src=javascript:alert(32311)>", true));
    assertEquals("abc", TextHtmlSanitizer.sanitizeText("abc<iframe src=javascript:alert(32311)>"));
  }

  @Test
  public void testOCRText() {
    String ocrText = "'No.) $i a7 bnchIL ky Altitude : i; od f am, — 2 — tS a ~ Ww a — = = < - ©\n" +
            "    3 = Yi J a = og = \"O O DAO PUNT 01- FLORA OF Locality & Habitat : Native Name:";
    assertTrue(TextHtmlSanitizer.isSafeText(ocrText));
    assertFalse(TextHtmlSanitizer.isSafeText(ocrText, false));
  }

  @ParameterizedTest
  @ValueSource(strings = {"© nye ) 4 4 i as f Fd 4 ag LO : oy ; wi Pye oA ad DAO i+<i TT 01-01558099",
          "HS Jackson&LT White", "Antrodia vaillantii <-"})
  public void testAcceptableText(String txt) {
    assertTrue(TextHtmlSanitizer.isAcceptableText(txt));
  }

  @ParameterizedTest
  @ValueSource(strings = {"© nye ) 4 4 i as f Fd 4 ag LO : oy ; wi Pye oA ad DAO <img src=no onerror=alert(1) foo= ",
          "abc <svg/onload=alert(1)",
          "<html><body><svg/onload=alert(123)</body></html>",
          "abc<iframe src=javascript:alert(32311)>",
          "© nye ) 4 4 i as f Fd 4 ag LO : oy ; wi Pye oA ad DAO i+<iframe src=javascript:alert(32311)"})
  public void testNotAcceptableText(String txt) {
    assertFalse(TextHtmlSanitizer.isAcceptableText(txt));
  }

}
