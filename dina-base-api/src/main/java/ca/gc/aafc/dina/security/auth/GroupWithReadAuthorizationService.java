package ca.gc.aafc.dina.security.auth;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import ca.gc.aafc.dina.security.DinaAuthenticatedUser;
import ca.gc.aafc.dina.security.spring.DinaPermissionEvaluator;
import ca.gc.aafc.dina.security.spring.MethodSecurityConfig;

/**
 *
 * Same as {@link GroupAuthorizationService} but the authorization is applied on the READ.
 *
 * Eventually {@link GroupAuthorizationService} will handle it but for now it may cause issues so we
 * have the 2 implementations.
 *
 */
@Service
public class GroupWithReadAuthorizationService extends PermissionAuthorizationService {

  /**
   * Proxy Method to invoke security authorization, Delegates to
   * {@link DinaPermissionEvaluator#hasGroupPermission(DinaAuthenticatedUser, Object)}.
   * This method itself does nothing, spring proxies must be called from a
   * separate bean. @PreAuthorize is only enabled with keycloak, see
   * {@link MethodSecurityConfig}. This method will not validate if keycloak is
   * disabled.
   *
   * @param entity
   */
  @Override
  @PreAuthorize("hasMinimumGroupAndRolePermissions(@currentUser, 'GUEST', #entity)")
  public void authorizeCreate(Object entity) {
  }

  /**
   * Proxy Method to invoke security authorization, Delegates to
   * {@link DinaPermissionEvaluator#hasGroupPermission(DinaAuthenticatedUser, Object)}.
   * This method itself does nothing, spring proxies must be called from a
   * separate bean. @PreAuthorize is only enabled with keycloak, see
   * {@link MethodSecurityConfig}. This method will not validate if keycloak is
   * disabled.
   *
   * @param entity
   */
  @Override
  @PreAuthorize("hasMinimumGroupAndRolePermissions(@currentUser, 'READ_ONLY', #entity)")
  public void authorizeRead(Object entity) {
  }

  /**
   * Proxy Method to invoke security authorization, Delegates to
   * {@link DinaPermissionEvaluator#hasGroupPermission(DinaAuthenticatedUser, Object)}.
   * This method itself does nothing, spring proxies must be called from a
   * separate bean. @PreAuthorize is only enabled with keycloak, see
   * {@link MethodSecurityConfig}. This method will not validate if keycloak is
   * disabled.
   *
   * @param entity
   */
  @Override
  @PreAuthorize("hasMinimumGroupAndRolePermissions(@currentUser, 'GUEST', #entity)")
  public void authorizeUpdate(Object entity) {
  }

  /**
   * Proxy Method to invoke security authorization, Delegates to
   * {@link DinaPermissionEvaluator#hasGroupPermission(DinaAuthenticatedUser, Object)}.
   * This method itself does nothing, spring proxies must be called from a
   * separate bean. @PreAuthorize is only enabled with keycloak, see
   * {@link MethodSecurityConfig}. This method will not validate if keycloak is
   * disabled.
   *
   * @param entity
   */
  @Override
  @PreAuthorize("hasMinimumGroupAndRolePermissions(@currentUser, 'USER', #entity)")
  public void authorizeDelete(Object entity) {
  }

  @Override
  public String getName() {
    return "GroupWithReadAuthorizationService";
  }
}

