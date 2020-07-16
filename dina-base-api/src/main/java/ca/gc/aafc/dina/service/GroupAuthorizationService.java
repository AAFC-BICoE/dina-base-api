package ca.gc.aafc.dina.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import ca.gc.aafc.dina.security.DinaAuthenticatedUser;
import ca.gc.aafc.dina.security.GroupBasedPermissionEvaluator;
import ca.gc.aafc.dina.security.MethodSecurityConfig;

/**
 * Authorization service using spring security. Service is only available on
 * keycloak.enabled = true. Use Optional injection when using this service.
 */
@Service
@ConditionalOnProperty(value = "keycloak.enabled", havingValue = "true")
public class GroupAuthorizationService implements DinaAuthorizationService {

  /**
   * Proxy Method to invoke security authorization, Delegates to
   * {@link GroupBasedPermissionEvaluator#hasDinaPermission(DinaAuthenticatedUser, Object)}.
   * This method itself does nothing, spring proxies must be called from a
   * seperate bean. @PreAuthorize is only enabled with keycloak, see
   * {@link MethodSecurityConfig}. This method will not validate if keycloak is
   * disabled.
   *
   * @param entity
   */
  @Override
  @PreAuthorize("hasDinaPermission(@currentUser, #entity)")
  public void authorizeCreate(Object entity) {
  }

  /**
   * Proxy Method to invoke security authorization, Delegates to
   * {@link GroupBasedPermissionEvaluator#hasDinaPermission(DinaAuthenticatedUser, Object)}.
   * This method itself does nothing, spring proxies must be called from a
   * seperate bean. @PreAuthorize is only enabled with keycloak, see
   * {@link MethodSecurityConfig}. This method will not validate if keycloak is
   * disabled.
   *
   * @param entity
   */
  @Override
  @PreAuthorize("hasDinaPermission(@currentUser, #entity)")
  public void authorizeUpdate(Object entity) {
  }

  /**
   * Proxy Method to invoke security authorization, Delegates to
   * {@link GroupBasedPermissionEvaluator#hasDinaPermission(DinaAuthenticatedUser, Object)}.
   * This method itself does nothing, spring proxies must be called from a
   * seperate bean. @PreAuthorize is only enabled with keycloak, see
   * {@link MethodSecurityConfig}. This method will not validate if keycloak is
   * disabled.
   *
   * @param entity
   */
  @Override
  @PreAuthorize("hasDinaPermission(@currentUser, #entity)")
  public void authorizeDelete(Object entity) {
  }

}
