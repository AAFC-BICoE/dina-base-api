package ca.gc.aafc.dina.security;

import java.util.List;
import java.util.Map;

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

  // Roles will be changed for an enum at some point
  private Map<String, List<String>> groupAndRole;

} 
