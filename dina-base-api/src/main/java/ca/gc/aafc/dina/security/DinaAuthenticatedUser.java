package ca.gc.aafc.dina.security;

import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

/**
 * Represent an authenticated user in the context of a DINA Module.
 * This class is immutable.
 */
@Builder
@Getter
public class DinaAuthenticatedUser {

  private UUID agentIdentifer;
  private String username;

} 
