package ca.gc.aafc.dina.security;

import java.util.Set;

import lombok.Builder;
import lombok.Getter;

/**
 * Represent an authenticated user in the context of a DINA Module.
 * This class is immutable.
 */
@Builder
@Getter
public class DinaAuthenticatedUser {

  private String agentIdentifer;
  private String username;
  private Set<String> groups;

} 
