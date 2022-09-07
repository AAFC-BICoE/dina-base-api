package ca.gc.aafc.dina.testsupport.security;

import org.apache.commons.lang3.StringUtils;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.adapters.OidcKeycloakAccount;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.adapters.springsecurity.account.SimpleKeycloakAccount;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.keycloak.representations.AccessToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Factory providing SecurityContext for tests annotated with {@link WithMockKeycloakUser}
 */
public class WithMockKeycloakSecurityContextFactory
    implements WithSecurityContextFactory<WithMockKeycloakUser> {

  // In production, keys (and claims) are set by Keycloak
  private static final String GROUPS_CLAIM_KEY = "groups";
  private static final String AGENT_IDENTIFIER_CLAIM_KEY = "agent-identifier";

  @Override
  public SecurityContext createSecurityContext(WithMockKeycloakUser mockKeycloakUser) {
    SecurityContext context = SecurityContextHolder.createEmptyContext();

    // Create a Keycloak AccessToken with the groups in otherClaims
    AccessToken accessToken = new AccessToken();

    List<String> groupRoles = Arrays.stream(mockKeycloakUser.groupRole())
        .map(gr -> convertToKeycloakNotation(gr, mockKeycloakUser.failOnInvalidNotation()))
        .collect(Collectors.toList());
    accessToken.setOtherClaims(GROUPS_CLAIM_KEY, groupRoles);

    if (StringUtils.isNotBlank(mockKeycloakUser.agentIdentifier())) {
      accessToken.setOtherClaims(AGENT_IDENTIFIER_CLAIM_KEY, mockKeycloakUser.agentIdentifier());
    }
    if (StringUtils.isNotBlank(mockKeycloakUser.internalIdentifier())) {
      accessToken.setSubject(mockKeycloakUser.internalIdentifier());
    }

    RefreshableKeycloakSecurityContext ctx = new RefreshableKeycloakSecurityContext(null, null,
        null, accessToken, null, null, null);

    KeycloakPrincipal<RefreshableKeycloakSecurityContext> principal = new KeycloakPrincipal<>(
        mockKeycloakUser.username(), ctx);

    OidcKeycloakAccount account = new SimpleKeycloakAccount(principal, Collections.emptySet(), ctx);

    Authentication auth = new KeycloakAuthenticationToken(account, false);
    context.setAuthentication(auth);
    return context;
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
    if(groupRoleParts.length != 2) {
      if(failOnInvalidNotation) {
        throw new IllegalArgumentException("Invalid groupRole notation. Excepted group:role.");
      }
      return "";
    }
    return StringUtils.prependIfMissing(groupRoleParts[0].strip(), "/") + "/" + groupRoleParts[1].strip();
  }
}
