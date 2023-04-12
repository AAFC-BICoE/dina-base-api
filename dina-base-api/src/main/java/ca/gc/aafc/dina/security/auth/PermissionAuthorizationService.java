package ca.gc.aafc.dina.security.auth;

import ca.gc.aafc.dina.security.auth.DinaAuthorizationService;
import ca.gc.aafc.dina.security.spring.SecurityChecker;

import javax.inject.Inject;
import java.util.Set;

/**
 * Authorization service that can return the permissions for an object. Extend this service to enable.
 */
public abstract class PermissionAuthorizationService implements DinaAuthorizationService {

  @Inject
  public SecurityChecker checker;

  @Override
  public Set<String> getPermissionsForObject(Object target) {
    return checker.getPermissionsForObject(target, this);
  }

}
