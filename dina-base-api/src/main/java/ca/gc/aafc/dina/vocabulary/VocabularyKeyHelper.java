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

  /**
   * Generates a key from a vocabulary element's name and type.
   * <p>
   * The key is constructed by first generating a base key from the provided name,
   * then appending the lowercase type name if the type is not STRING.
   * For STRING types, only the name-based key is returned.
   * </p>
   *
   * @param name the name of the vocabulary element; must not be null
   * @param type the type of the vocabulary element; must not be null
   * @return a key combining the name and type information
   *
   * @see #generateKeyFromName(String)
   * @see TypedVocabularyElement.VocabularyElementType
   */
  public static String generateKeyFromNameAndType(String name,
                                                  TypedVocabularyElement.VocabularyElementType type) {
    Objects.requireNonNull(name);
    Objects.requireNonNull(type);

    String key = generateKeyFromName(name);
    return type != TypedVocabularyElement.VocabularyElementType.STRING ?
      key + "_" + type.name().toLowerCase() : key;
  }

  private static String processName(String name) {
    return RegExUtils.removeAll(name.toLowerCase(), NON_ALPHANUMERICAL);
  }

  public static boolean isKeyValid(String key) {
    if (StringUtils.isBlank(key)) {
      return false;
    }
    // use the key as the name and check if we get the same result
    return key.equals(generateKeyFromName(key));
  }

}
