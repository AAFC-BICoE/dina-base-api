package ca.gc.aafc.dina.security;

import java.util.Map;
import java.util.Set;

import lombok.Builder;
import lombok.Getter;

/**
 * Represent an authenticated user in the context of a DINA Module. This class
 * is immutable.
 */
@Getter
public class DinaAuthenticatedUser {

  private final String agentIdentifer;
  private final String username;

  private final Set<String> groups;

  private final Map<String, Set<DinaRole>> rolesPerGroup;

  @Builder
  public DinaAuthenticatedUser(String username, String agentIdentifer, Map<String, Set<DinaRole>> rolesPerGroup) {
    this.username = username;
    this.agentIdentifer = agentIdentifer;
    this.rolesPerGroup = rolesPerGroup;

    this.groups = rolesPerGroup.keySet();
  }
}
