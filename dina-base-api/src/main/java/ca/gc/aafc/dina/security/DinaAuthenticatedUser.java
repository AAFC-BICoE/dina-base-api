package ca.gc.aafc.dina.security;

import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Represent an authenticated user in the context of a DINA Module. This class is immutable.
 */
@Getter
public class DinaAuthenticatedUser {

  private final String internalIdentifer;
  private final String agentIdentifer;
  private final String username;
  private final Set<String> groups;
  private final Map<String, Set<DinaRole>> rolesPerGroup;

  @Builder
  public DinaAuthenticatedUser(
    String username,
    String agentIdentifier,
    String internalIdentifier,
    Map<String, Set<DinaRole>> rolesPerGroup
  ) {
    this.internalIdentifer = internalIdentifier;
    this.username = username;
    this.agentIdentifer = agentIdentifier;
    this.rolesPerGroup = rolesPerGroup == null ? Collections.emptyMap() : rolesPerGroup;
    this.groups = this.rolesPerGroup.keySet();
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
}
