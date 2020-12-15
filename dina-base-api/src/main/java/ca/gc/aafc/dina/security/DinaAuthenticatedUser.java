package ca.gc.aafc.dina.security;

import lombok.Builder;
import lombok.Getter;

import java.util.Collections;
import java.util.Map;
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
    String agentIdentifer,
    String internalIdentifer,
    Map<String, Set<DinaRole>> rolesPerGroup
  ) {
    this.internalIdentifer = internalIdentifer;
    this.username = username;
    this.agentIdentifer = agentIdentifer;
    this.rolesPerGroup = rolesPerGroup == null ? Collections.emptyMap() : rolesPerGroup;
    this.groups = this.rolesPerGroup.keySet();
  }
}
