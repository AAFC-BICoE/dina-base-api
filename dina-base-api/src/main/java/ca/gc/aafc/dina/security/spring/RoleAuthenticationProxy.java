package ca.gc.aafc.dina.security.spring;

import ca.gc.aafc.dina.security.DinaAuthenticatedUser;
import ca.gc.aafc.dina.security.DinaRole;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Service proxy to route calls to {@link DinaPermissionEvaluator} with the applications
 * Authenticated User. Generally Given to a {@link ca.gc.aafc.dina.service.RoleAuthorizationService}
 * to restrict operations. Available in the application context unless keycloak is disabled.
 */
@Service
@ConditionalOnProperty(value = "keycloak.enabled", matchIfMissing = true)
public class RoleAuthenticationProxy {

  /**
   * Proxy Method to invoke security authorization, Delegates to {@link
   * DinaPermissionEvaluator#hasDinaRole(DinaAuthenticatedUser, Set)}. This method itself does
   * nothing, spring proxies must be called from a separate bean. @PreAuthorize is only enabled with
   * keycloak, see {@link MethodSecurityConfig}. This method will not validate if keycloak is
   * disabled.
   *
   * @param roles - roles to compare against current user
   */
  @PreAuthorize("hasDinaRole(@currentUser, #roles)")
  public void hasDinaRole(Set<DinaRole> roles) {
  }

}
