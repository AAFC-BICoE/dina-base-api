package ca.gc.aafc.dina.security;

import ca.gc.aafc.dina.security.spring.SecurityChecker;

import javax.inject.Inject;
import java.util.Set;

public abstract class PermissionAuthorizationService implements DinaAuthorizationService {

  @Inject
  public SecurityChecker checker;

  @Override
  public Set<String> getPermissionsForObject(Object target) {
    return checker.getPermissionsForObject(target, this);
  }

}
