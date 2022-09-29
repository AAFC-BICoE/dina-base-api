package ca.gc.aafc.dina.security;

import org.junit.jupiter.api.Test;

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
    assertEquals("abc", TextHtmlSanitizer.sanitizeText("abc<iframe src=javascript:alert(32311)>"));
  }

}
