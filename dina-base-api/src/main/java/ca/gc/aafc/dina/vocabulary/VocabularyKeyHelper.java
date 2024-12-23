package ca.gc.aafc.dina.vocabulary;

import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Utility class to have uniform handling of key generation from a name.
 */
public final class VocabularyKeyHelper {

  private static final Pattern NON_ALPHANUMERICAL = Pattern.compile("[^a-z0-9]");

  private VocabularyKeyHelper() {
    // utility class
  }

  /**
   * Transforms a name into a key. camelCase is supported.
   * "Aa bb !! mySuperAttribute # 11" will become aa_bb_my_super_attribute_11
   * @param name
   * @return
   */
  public static String generateKeyFromName(String name) {
    Objects.requireNonNull(name);

    return Arrays.stream(StringUtils.
        splitByCharacterTypeCamelCase(StringUtils.normalizeSpace(name)))
      .filter(StringUtils::isNotBlank)
      .map(VocabularyKeyHelper::processName)
      .filter(StringUtils::isNotBlank)
      .collect(Collectors.joining("_"));
  }

  private static String processName(String name) {
    return RegExUtils.removeAll(name.toLowerCase(), NON_ALPHANUMERICAL);
  }

}
