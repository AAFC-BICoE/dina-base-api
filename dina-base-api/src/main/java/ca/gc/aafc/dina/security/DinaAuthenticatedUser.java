package ca.gc.aafc.dina.security;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Represent an authenticated user in the context of a DINA Module.
 * This class is immutable.
 */
@AllArgsConstructor
@Getter
public class DinaAuthenticatedUser {

  private UUID agentIdentifer;

}