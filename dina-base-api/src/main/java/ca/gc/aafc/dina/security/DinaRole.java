package ca.gc.aafc.dina.security;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Represent user role in the context of a DINA module.
 */
@RequiredArgsConstructor
public enum DinaRole {

  DINA_ADMIN("dina-admin", 0),
  COLLECTION_MANAGER("collection-manager", 1),
  STAFF("staff", 2),
  STUDENT("student", 3);
  
  private static final Pattern NON_ALPHA = Pattern.compile("[^A-Za-z]");

  /**
   * Name as entered in Keycloak
   */
  @Getter
  private final String keycloakRoleName;
  
  /**
   * Priority of the role, lower number = higher priority
   */
  private final int priority;

  /**
   * Similar but more lenient than {@link #valueOf(String)}.
   * String like "collection-manager" will match COLLECTION_MANAGER.
   * @param str
   * @return
   */
  public static Optional<DinaRole> fromString(String str) {
    for (DinaRole currRole : values()) {
      if (currRole.name().equalsIgnoreCase(NON_ALPHA.matcher(str).replaceAll("_"))) {
        return Optional.of(currRole);
      }
    }
    return Optional.empty();
  }

  public boolean isHigherThan(@NonNull DinaRole dinaRole) {
    return priority < dinaRole.priority;
  }

  public boolean isHigherOrEqualThan(@NonNull DinaRole dinaRole) {
    return priority <= dinaRole.priority;
  }

}
