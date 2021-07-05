package ca.gc.aafc.dina.security;

import ca.gc.aafc.dina.security.spring.SecurityChecker;

import java.util.Set;

/**
 * Authorization service that allows all operations.
 */
public class AllowAllAuthorizationService implements DinaAuthorizationService {

  @Override
  public void authorizeCreate(Object entity) {
    // do nothing
  }

  @Override
  public void authorizeUpdate(Object entity) {
    // do nothing
  }

  @Override
  public void authorizeDelete(Object entity) {
    // do nothing
  }

  @Override
  public Set<String> getPermissionsForObject(Object target) {
    return Set.of(
      SecurityChecker.Operations.CREATE.getValue(),
      SecurityChecker.Operations.DELETE.getValue(),
      SecurityChecker.Operations.UPDATE.getValue()
    );
  }

}
