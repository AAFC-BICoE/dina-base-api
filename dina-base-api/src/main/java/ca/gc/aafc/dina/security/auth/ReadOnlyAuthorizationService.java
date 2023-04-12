package ca.gc.aafc.dina.security.auth;

import java.util.Set;

import ca.gc.aafc.dina.security.auth.DinaAuthorizationService;
import ca.gc.aafc.dina.security.spring.SecurityChecker.Operations;

/**
 * To be used with the {@link ca.gc.aafc.dina.repository.ReadOnlyDinaRepository} to return a correct set of
 * permissions. Does not authorize and will return only read permission.
 */
public class ReadOnlyAuthorizationService implements DinaAuthorizationService {

  public static final Set<String> READ_ONLY_PERMISSION = Set.of(Operations.READ.getValue());

  @Override
  public void authorizeCreate(Object entity) {
  }

  @Override
  public void authorizeRead(Object entity) {
  }

  @Override
  public void authorizeUpdate(Object entity) {
  }

  @Override
  public void authorizeDelete(Object entity) {
  }

  @Override
  public Set<String> getPermissionsForObject(Object target) {
    return READ_ONLY_PERMISSION;
  }

  @Override
  public String getName() {
    return "ReadOnlyAuthorizationService";
  }

}
