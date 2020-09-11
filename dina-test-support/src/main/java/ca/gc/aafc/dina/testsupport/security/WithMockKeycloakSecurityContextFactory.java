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

public class WithMockKeycloakSecurityContextFactory
    implements WithSecurityContextFactory<WithMockKeycloakUser> {

  @Override
  public SecurityContext createSecurityContext(WithMockKeycloakUser mockKeycloakUser) {
    SecurityContext context = SecurityContextHolder.createEmptyContext();

    // Create a Keycloak AccessToken with the groups in otherClaims
    AccessToken accessToken = new AccessToken();

    List<String> groupRoles = Arrays.stream(mockKeycloakUser.groupRole())
        .map(s -> StringUtils.prependIfMissing(StringUtils.replace(s, ":", "/"), "/"))
        .collect(Collectors.toList());
    accessToken.setOtherClaims("groups", groupRoles);

    RefreshableKeycloakSecurityContext ctx = new RefreshableKeycloakSecurityContext(null, null,
        null, accessToken, null, null, null);

    KeycloakPrincipal<RefreshableKeycloakSecurityContext> principal = new KeycloakPrincipal<>(
        mockKeycloakUser.username(), ctx);

    OidcKeycloakAccount account = new SimpleKeycloakAccount(principal, Collections.emptySet(), ctx);

    Authentication auth = new KeycloakAuthenticationToken(account, false);
    context.setAuthentication(auth);
    return context;
  }
}
