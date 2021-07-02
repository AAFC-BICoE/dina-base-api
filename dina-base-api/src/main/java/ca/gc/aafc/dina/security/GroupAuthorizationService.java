package ca.gc.aafc.dina.security;

import ca.gc.aafc.dina.security.spring.DinaPermissionEvaluator;
import ca.gc.aafc.dina.security.spring.MethodSecurityConfig;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * Authorization service using spring security. Service will only proxy authorization methods on
 * keycloak.enabled = true.
 */
@Service
public class GroupAuthorizationService implements DinaAuthorizationService {

  /**
   * Proxy Method to invoke security authorization, Delegates to
   * {@link DinaPermissionEvaluator#hasGroupPermission(DinaAuthenticatedUser, Object)}.
   * This method itself does nothing, spring proxies must be called from a
   * seperate bean. @PreAuthorize is only enabled with keycloak, see
   * {@link MethodSecurityConfig}. This method will not validate if keycloak is
   * disabled.
   *
   * @param entity
   */
  @Override
  @PreAuthorize("hasGroupPermission(@currentUser, #entity)")
  public void authorizeCreate(Object entity) {
  }

  /**
   * Proxy Method to invoke security authorization, Delegates to
   * {@link DinaPermissionEvaluator#hasGroupPermission(DinaAuthenticatedUser, Object)}.
   * This method itself does nothing, spring proxies must be called from a
   * seperate bean. @PreAuthorize is only enabled with keycloak, see
   * {@link MethodSecurityConfig}. This method will not validate if keycloak is
   * disabled.
   *
   * @param entity
   */
  @Override
  @PreAuthorize("hasGroupPermission(@currentUser, #entity)")
  public void authorizeUpdate(Object entity) {
  }

  /**
   * Proxy Method to invoke security authorization, Delegates to
   * {@link DinaPermissionEvaluator#hasGroupPermission(DinaAuthenticatedUser, Object)}.
   * This method itself does nothing, spring proxies must be called from a
   * seperate bean. @PreAuthorize is only enabled with keycloak, see
   * {@link MethodSecurityConfig}. This method will not validate if keycloak is
   * disabled.
   *
   * @param entity
   */
  @Override
  @PreAuthorize("hasGroupPermission(@currentUser, #entity)")
  public void authorizeDelete(Object entity) {
  }

}
