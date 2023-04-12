package ca.gc.aafc.dina.security.auth;

import ca.gc.aafc.dina.security.DinaAuthenticatedUser;
import ca.gc.aafc.dina.security.spring.DinaPermissionEvaluator;
import ca.gc.aafc.dina.security.spring.MethodSecurityConfig;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * Authorization Service that checks the group of an object and compare it with the group membership of the user.
 * The minimum role is also checked.
 *
 * Built on Spring Security. Service will only proxy authorization methods on
 * keycloak.enabled = true, see {@link MethodSecurityConfig}.
 *
 */
@Service
public class GroupAuthorizationService extends PermissionAuthorizationService {

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
    return "GroupAuthorizationService";
  }
}
