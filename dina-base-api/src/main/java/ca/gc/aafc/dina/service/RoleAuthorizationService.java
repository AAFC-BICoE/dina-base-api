package ca.gc.aafc.dina.service;

import ca.gc.aafc.dina.security.DinaRole;
import ca.gc.aafc.dina.security.spring.RoleAuthorizationProxy;
import lombok.RequiredArgsConstructor;

import java.util.Set;

/**
 * Authorization service to limit operations based off a given set of roles. A {@link
 * RoleAuthorizationProxy} from the applications context should be given during construction.
 * {@link RoleAuthorizationProxy} is available in the application context unless keycloak is
 * disabled.
 */
@RequiredArgsConstructor
public class RoleAuthorizationService implements DinaAuthorizationService {

  private final RoleAuthorizationProxy proxy;
  private final Set<DinaRole> roles;

  /**
   * Delegates to {@link RoleAuthorizationProxy#hasDinaRole(Set)}, using this classes given roles.
   */
  @Override
  public void authorizeCreate(Object entity) {
    proxy.hasDinaRole(this.roles);
  }

  /**
   * Delegates to {@link RoleAuthorizationProxy#hasDinaRole(Set)}, using this classes given roles.
   */
  @Override
  public void authorizeUpdate(Object entity) {
    proxy.hasDinaRole(this.roles);
  }

  /**
   * Delegates to {@link RoleAuthorizationProxy#hasDinaRole(Set)}, using this classes given roles.
   */
  @Override
  public void authorizeDelete(Object entity) {
    proxy.hasDinaRole(this.roles);
  }

}
