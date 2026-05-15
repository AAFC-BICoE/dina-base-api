package ca.gc.aafc.dina.testsupport.security;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Factory providing SecurityContext for tests annotated with {@link WithMockKeycloakUser}
 */
public class WithMockKeycloakSecurityContextFactory
    implements WithSecurityContextFactory<WithMockKeycloakUser> {

  // In production, keys (and claims) are set by Keycloak
  private static final String GROUPS_CLAIM_KEY = "groups";
  private static final String AGENT_IDENTIFIER_CLAIM_KEY = "agent-identifier";
  private static final String REALM_ACCESS_CLAIM_KEY = "realm_access";
  private static final String ROLES_CLAIM_KEY = "roles";

  @Override
  public SecurityContext createSecurityContext(WithMockKeycloakUser mockKeycloakUser) {
    SecurityContext context = SecurityContextHolder.createEmptyContext();

    // Build claims map
    Map<String, Object> claims = new HashMap<>();
    claims.put("sub", StringUtils.isNotBlank(mockKeycloakUser.internalIdentifier())
      ? mockKeycloakUser.internalIdentifier()
      : "default-subject");
    claims.put("preferred_username", mockKeycloakUser.username());

    // Add groups claim
    if (mockKeycloakUser.groupRole() != null && mockKeycloakUser.groupRole().length > 0 &&
      StringUtils.isNotBlank(mockKeycloakUser.groupRole()[0])) {
      List<String> groupRoles = Arrays.stream(mockKeycloakUser.groupRole())
        .map(gr -> convertToKeycloakNotation(gr, mockKeycloakUser.failOnInvalidNotation()))
        .collect(Collectors.toList());
      claims.put(GROUPS_CLAIM_KEY, groupRoles);
    }

    // Add agent identifier claim
    if (StringUtils.isNotBlank(mockKeycloakUser.agentIdentifier())) {
      claims.put(AGENT_IDENTIFIER_CLAIM_KEY, mockKeycloakUser.agentIdentifier());
    }

    // Add realm_access roles
    if (mockKeycloakUser.adminRole() != null && mockKeycloakUser.adminRole().length > 0) {
      Map<String, Object> realmAccess = new HashMap<>();
      realmAccess.put(ROLES_CLAIM_KEY, Arrays.asList(mockKeycloakUser.adminRole()));
      claims.put(REALM_ACCESS_CLAIM_KEY, realmAccess);
    }

    // Create JWT token
    Jwt jwt = createJwt(mockKeycloakUser.username(), claims);

    // Extract authorities from roles
    Collection<GrantedAuthority> authorities = extractAuthorities(mockKeycloakUser.adminRole());

    // Create authentication token
    Authentication auth = new JwtAuthenticationToken(jwt, authorities, "preferred_username");
    context.setAuthentication(auth);
    return context;
  }

  /**
   * Create a Jwt token with the given subject and claims
   */
  private Jwt createJwt(String username, Map<String, Object> claims) {
    Instant now = Instant.now();
    return new Jwt(
      "mock-token",                    // tokenValue
      now,                             // issuedAt
      now.plusSeconds(3600),           // expiresAt
      Collections.singletonMap("alg", "none"),  // headers
      claims                           // claims
    );
  }

  /**
   * Extract GrantedAuthorities from admin roles, prefixing with ROLE_
   */
  private Collection<GrantedAuthority> extractAuthorities(String[] adminRoles) {
    if (adminRoles == null || adminRoles.length == 0) {
      return Collections.emptyList();
    }
    return Arrays.stream(adminRoles)
      .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
      .collect(Collectors.toList());
  }

  /**
   * Utility method to convert dina testing notation (group:role) to Keycloak notation (/group/role)
   * Ex: group 2:dina-admin -> /group 2/dina-admin
   * @param groupRole
   * @param failOnInvalidNotation should we throw an exception if the notation is invalid
   * @return
   */
  private static String convertToKeycloakNotation(String groupRole, boolean failOnInvalidNotation) {
    String[] groupRoleParts = StringUtils.split(groupRole, ":");
    if (groupRoleParts.length != 2) {
      if (failOnInvalidNotation) {
        throw new IllegalArgumentException("Invalid groupRole notation. Excepted group:role.");
      }
      return "";
    }
    return StringUtils.prependIfMissing(groupRoleParts[0].strip(), "/") + "/" + groupRoleParts[1].strip();
  }
}
