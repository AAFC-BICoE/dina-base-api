package ca.gc.aafc.dina.security;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.Comparator;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Represent user role in the context of a DINA module.
 */
@RequiredArgsConstructor
public enum DinaRole {

  DINA_ADMIN("dina-admin", 0),
  SUPER_USER("super-user", 1),
  STAFF("staff", 2),
  STUDENT("student", 3),
  READ_ONLY("read-only", 4);

  /**
   * Read carefully since sorting is done based on priority:
   * compare(x,y) Returns -1 if x higher in priority than y, 0 if they are equal, and 1 if x is less in priority.
   */
  public static final Comparator<DinaRole> COMPARATOR = Comparator.comparingInt(DinaRole::getPriority);

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
   * String like "super-user" will match SUPER_USER.
   * @param str string representing the role.
   * @return {@link DinaRole} as {@link Optional} or {@link Optional#empty()} if not found.
   */
  public static Optional<DinaRole> fromString(String str) {
    if (StringUtils.isBlank(str)) {
      return Optional.empty();
    }

    String standardizedRoleName = NON_ALPHA.matcher(str).replaceAll("_");
    for (DinaRole currRole : values()) {
      if (currRole.name().equalsIgnoreCase(standardizedRoleName)) {
        return Optional.of(currRole);
      }
    }
    return Optional.empty();
  }

  /**
   * Private function. Use {@link #COMPARATOR} or specific methods.
   * @return
   */
  private int getPriority() {
    return priority;
  }

  public boolean isHigherThan(@NonNull DinaRole dinaRole) {
    return priority < dinaRole.priority;
  }

  public boolean isHigherOrEqualThan(@NonNull DinaRole dinaRole) {
    return priority <= dinaRole.priority;
  }

}
