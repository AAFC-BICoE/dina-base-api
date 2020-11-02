package ca.gc.aafc.dina.security;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Represent user role in the context of a DINA module.
 */
public enum DinaRole {

  COLLECTION_MANAGER,
  STAFF,
  STUDENT;

  private static final Pattern NON_ALPHA = Pattern.compile("[^A-Za-z]");

  /**
   * Similar but more lenient than {@link #valueOf(String)}.
   * String like "collection-manager" will match COLLECTION_MANAGER.
   * @param str
   * @return
   */
  static Optional<DinaRole> fromString(String str) {
    for (DinaRole currRole : values()) {
      if (currRole.name().equalsIgnoreCase(NON_ALPHA.matcher(str).replaceAll("_"))) {
        return Optional.of(currRole);
      }
    }
    return Optional.empty();
  }
}
