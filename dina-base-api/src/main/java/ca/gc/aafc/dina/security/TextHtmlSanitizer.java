package ca.gc.aafc.dina.security;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Safelist;

/**
 * Utility class to check and sanitize text received from the user in case it could be unsafe.
 * Safe means safe to display on a html page.
 */
public final class TextHtmlSanitizer {

  private static final Safelist NONE = Safelist.none();
  private static final Safelist BASIC = Safelist.basic();

  private static final int HTML_SHELL_SIZE = Document.createShell("").getAllElements().size();
  private static final int MAX_ERROR_TRACKED = 5;
  private static final String CONDITIONAL_ACCEPTED_PARSE_ERROR = "Unexpectedly reached end of file (EOF)";

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

  /**
   * Check if the provided text can be considered as safe for html.
   * allowUnescapedEntities is set to true
   * @param txt
   * @return
   */
  public static boolean isSafeText(String txt) {
    return isSafeText(txt, NONE, true);
  }

  public static boolean isSafeText(String txt, boolean allowUnescapedEntities) {
    return isSafeText(txt, NONE, allowUnescapedEntities);
  }

  /**
   * Check if the text is safe to use in HTML according to the Safelist.
   * Optionally, the check can skip unescapedEntities (e.g. <, > ) if the text will be used in something else than html.
   * @param txt the text input
   * @param safelist JSoup Safelist instance
   * @param allowUnescapedEntities should unescaped entities be identified as safe or no
   * @return can the text be considered safe or not
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

  /**
   * Check if the text can be considered acceptable. Acceptable does NOT mean safe.
   * The result should still be used with caution and proper escaping in html.
   *
   * Acceptable is defined by a text that only contains unclosed tag without creating additional html elements.
   * If they were to prefix another html element it should only create an element from the Basic Safelist.
   *
   * @param txt
   * @return
   */
  public static boolean isAcceptableText(String txt) {
    Parser p = Parser.htmlParser();
    p.setTrackErrors(MAX_ERROR_TRACKED);
    Document d = p.parseInput(txt, "");

    // if a single element is added it should be rejected
    if(d.getAllElements().size() != HTML_SHELL_SIZE) {
      return false;
    }

    // if we reached the maximum number of errors it should be rejected
    if(p.getErrors().size() == MAX_ERROR_TRACKED) {
      return false;
    }

    // if some parsing errors are not in the accepted list it should be rejected
    if (!p.getErrors().stream()
            .allMatch(pe -> StringUtils.startsWith(pe.getErrorMessage(), CONDITIONAL_ACCEPTED_PARSE_ERROR))) {
      return false;
    }

    // check the impact of prefixing the txt with a paragraph
    return isFollowedByParagraphOnlyCreatesBasicElement(txt);
  }

  /**
   * Tries to evaluate the impact of having the acceptable text before a html paragraph.
   * The browser may use the paragraph to close tags in the provided txt. We allow it if the impact is still passing the SafeList BASIC.
   * @param txt
   * @return
   */
  private static boolean isFollowedByParagraphOnlyCreatesBasicElement(String txt) {
    Parser p = Parser.htmlParser();
    Document d = p.parseInput(txt + "<p>abc</p>", "");

    Cleaner c = new Cleaner(BASIC);
    Document dd = c.clean(d);

    // if a single element is cleaned it should be rejected
    return d.getAllElements().size() == dd.getAllElements().size();
  }
}
