package ca.gc.aafc.dina.service;

import java.util.Set;

import ca.gc.aafc.dina.security.DinaAuthenticatedUser;
import ca.gc.aafc.dina.security.DinaRole;
import io.crnk.core.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RoleAuthorizationService implements DinaAuthorizationService {

  private final DinaRole role;
  private final DinaAuthenticatedUser user;

  @Override
  public void authorizeCreate(Object entity) {
    this.authorize();
  }

  @Override
  public void authorizeUpdate(Object entity) {
    this.authorize();
  }

  @Override
  public void authorizeDelete(Object entity) {
    this.authorize();
  }

  private void authorize() {
    if(!RoleAuthorizationService.hasDinaRole(this.user, this.role)){
      throw new ForbiddenException("");
    }
  }

  private static boolean hasDinaRole(DinaAuthenticatedUser user, DinaRole role) {
    if (user == null || role == null) {
      return false;
    }
    return user.getRolesPerGroup().values().stream().flatMap(Set::stream).anyMatch(role::equals);
  }

}
