package ca.gc.aafc.dina.security;

import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represent an authenticated user in the context of a DINA Module. This class is immutable.
 */
@Getter
public class DinaAuthenticatedUser {

  private final String internalIdentifier;
  private final String agentIdentifier;
  private final String username;
  private final Set<String> groups;
  private final Map<String, Set<DinaRole>> rolesPerGroup;
  private final Set<DinaRole> adminRoles;
  private final boolean isServiceAccount;

  @Builder
  public DinaAuthenticatedUser(
    String username,
    String agentIdentifier,
    String internalIdentifier,
    Map<String, Set<DinaRole>> rolesPerGroup,
    Set<DinaRole> adminRoles,
    boolean isServiceAccount
  ) {
    this.internalIdentifier = internalIdentifier;
    this.username = username;
    this.agentIdentifier = agentIdentifier;
    this.rolesPerGroup = rolesPerGroup == null ? Collections.emptyMap() : rolesPerGroup;
    this.groups = this.rolesPerGroup.keySet();
    this.adminRoles = adminRoles == null ? Set.of() : adminRoles;
    this.isServiceAccount = isServiceAccount;
  }

  /**
   * Returns the Set of dina roles this user has for the given group, empty if the user has no roles for this
   * group, or the group is blank.
   *
   * @param group group to search
   * @return the Set of dina roles this user has for the given group
   */
  public Optional<Set<DinaRole>> getRolesForGroup(String group) {
    if (StringUtils.isBlank(group) || rolesPerGroup.isEmpty()) {
      return Optional.empty();
    }

    return this.rolesPerGroup.entrySet().stream()
      .filter(rolePerGroup -> rolePerGroup.getKey().equalsIgnoreCase(group.strip()))
      .map(Map.Entry::getValue)
      .findFirst();
  }

  /**
   * Given a minimumRole, returns the group where the user has this role or higher.
   *
   * @param minimumRole
   * @return Set of groups where the user has the minimumRole or higher. Otherwise, empty set.
   */
  public Set<String> getGroupsForMinimumRole(DinaRole minimumRole) {
    return this.rolesPerGroup.entrySet()
            .stream().filter(es -> hasMinimumRoleOrHigher(es.getValue(), minimumRole))
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
  }

  /**
   * Checks if the set of roles contains a minimum role or higher.
   * @param roles
   * @param minimumRole
   * @return
   */
  private static boolean hasMinimumRoleOrHigher(Set<DinaRole> roles, DinaRole minimumRole) {
    for (DinaRole role : roles) {
      if (role.isHigherOrEqualThan(minimumRole)) {
        return true;
      }
    }
    return false;
  }
}
