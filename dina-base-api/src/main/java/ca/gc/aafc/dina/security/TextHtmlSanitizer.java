package ca.gc.aafc.dina.security;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

/**
 * Utility class to check and sanitize text received from the user in case it could be unsafe.
 * Safe means safe to display on a html page.
 */
public final class TextHtmlSanitizer {

  private static final Safelist NONE = Safelist.none();

  private TextHtmlSanitizer() {
    //utility class
  }

  /**
   * Sanitize value received from the user to make sure it is safe to return it.
   * @param txt
   * @return
   */
  public static String sanitizeText(String txt) {
    return Jsoup.clean(txt, NONE);
  }

  public static boolean isSafeText(String txt) {
    return Jsoup.isValid(txt, NONE);
  }
}
