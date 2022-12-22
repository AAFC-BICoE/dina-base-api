package ca.gc.aafc.dina.security;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.parser.Parser;
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
    if (StringUtils.isBlank(txt)) {
      return txt;
    }
    return Jsoup.clean(txt, NONE);
  }

  public static boolean isSafeText(String txt) {
    return isSafeText(txt, NONE, false);
  }

  public static boolean isSafeText(String txt, boolean allowUnescapedEntities) {
    return isSafeText(txt, NONE, allowUnescapedEntities);
  }

  /**
   * Check if the text is safe to use in HTML according to the Safelist.
   * Optionally, the check can skip unescapedEntities (e.g. <, > ) if the text will be used in something else than html.
   * @param txt the text input
   * @param allowUnescapedEntities should unescaped entities be identified as safe or no
   * @return
   */
  public static boolean isSafeText(String txt, Safelist safelist, boolean allowUnescapedEntities) {
    if (StringUtils.isBlank(txt)) {
      return true;
    }

    if(Jsoup.isValid(txt, safelist)) {
      return true;
    }

    // make sure that the unescaped entities are not part of an unsafe html so, we sanitize the input first.
    if(allowUnescapedEntities) {
      return StringUtils.normalizeSpace(txt).equals(Parser.unescapeEntities(TextHtmlSanitizer.sanitizeText(txt), false));
    }
    return false;
  }
}
