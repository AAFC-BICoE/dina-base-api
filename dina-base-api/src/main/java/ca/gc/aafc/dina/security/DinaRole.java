package ca.gc.aafc.dina.security;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.List;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.Comparator;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Represent user role in the context of a DINA module.
 * The roles that end with _ADMIN mean that they are admin-based so not restricted by group.
 */
@SuppressFBWarnings(value = "MS_EXPOSE_REP")
@RequiredArgsConstructor
public enum DinaRole {

  DINA_ADMIN("dina-admin", 0, true),
  SUPER_USER("super-user", 1, false),
  USER("user", 2, false),
  GUEST("guest", 3, false),
  READ_ONLY_ADMIN("read-only-admin", 4, true), // for service accounts like search-cli
  READ_ONLY("read-only", 5, false);

  /**
   * Read carefully since sorting is done based on priority:
   * compare(x,y) Returns -1 if x higher in priority than y, 0 if they are equal, and 1 if x is less in priority.
   */
  public static final Comparator<DinaRole> COMPARATOR = Comparator.comparingInt(DinaRole::getPriority);

  private static final Pattern NON_ALPHA = Pattern.compile("[^A-Za-z]");

  private static final List<DinaRole> ADMIN_BASED_ROLES = Arrays.stream(DinaRole.values())
    .filter(DinaRole::isAdminBased)
    .toList();

  private static final List<DinaRole> GROUP_BASED_ROLES = Arrays.stream(DinaRole.values())
    .filter(r -> !r.isAdminBased())
    .toList();

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
   * Is a role admin-based or not. admin-base roles are not restricted by group.
   */
  @Getter
  private final boolean adminBased;

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
   * List of roles that are group-based.
   * @return
   */
  public static List<DinaRole> groupBasedRoles() {
    return GROUP_BASED_ROLES;
  }

  /**
   * List of roles that are admin-based.
   * @return
   */
  public static List<DinaRole> adminBasedRoles() {
    return ADMIN_BASED_ROLES;
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
