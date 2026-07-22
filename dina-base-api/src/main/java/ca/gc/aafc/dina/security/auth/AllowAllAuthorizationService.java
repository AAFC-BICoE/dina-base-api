package ca.gc.aafc.dina.security.auth;

import java.util.Set;

import org.springframework.security.access.prepost.PreAuthorize;

import ca.gc.aafc.dina.security.spring.SecurityChecker;

/**
 * Authorization service that allows all operations.
 */
public class AllowAllAuthorizationService implements DinaAuthorizationService {

  @Override
  @PreAuthorize("allow()")
  public void authorizeCreate(Object entity) {
    // do nothing
  }

  @Override
  @PreAuthorize("allow()")
  public void authorizeRead(Object entity) {
    // do nothing
  }

  @Override
  @PreAuthorize("allow()")
  public void authorizeUpdate(Object entity) {
    // do nothing
  }

  @Override
  @PreAuthorize("allow()")
  public void authorizeDelete(Object entity) {
    // do nothing
  }

  @Override
  public Set<String> getPermissionsForObject(Object target) {
    return Set.of(
      SecurityChecker.Operations.CREATE.getValue(),
      SecurityChecker.Operations.READ.getValue(),
      SecurityChecker.Operations.DELETE.getValue(),
      SecurityChecker.Operations.UPDATE.getValue()
    );
  }

  @Override
  public String getName() {
    return "AllowAllAuthorizationService";
  }

}
