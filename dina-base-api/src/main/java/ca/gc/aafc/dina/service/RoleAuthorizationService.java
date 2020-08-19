package ca.gc.aafc.dina.service;

import ca.gc.aafc.dina.security.DinaRole;
import ca.gc.aafc.dina.security.spring.RoleAuthenticationProxy;
import lombok.RequiredArgsConstructor;

import java.util.Set;

@RequiredArgsConstructor
public class RoleAuthorizationService implements DinaAuthorizationService {

  private final RoleAuthenticationProxy proxy;
  private final Set<DinaRole> roles;

  @Override
  public void authorizeCreate(Object entity) {
    proxy.hasDinaRole(this.roles);
  }

  @Override
  public void authorizeUpdate(Object entity) {
    proxy.hasDinaRole(this.roles);
  }

  @Override
  public void authorizeDelete(Object entity) {
    proxy.hasDinaRole(this.roles);
  }

}
