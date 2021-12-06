package ca.gc.aafc.dina.security;

import java.util.HashSet;
import java.util.Set;

import ca.gc.aafc.dina.security.spring.SecurityChecker.Operations;

/**
 * To be used with the {@link ca.gc.aafc.dina.repository.ReadOnlyDinaRepository} to return a correct set of
 * permissions. Does not authorize and will return only read permission.
 */
public class ReadOnlyAuthorizationService implements DinaAuthorizationService {

  public static final Set<String> NO_PERMISSIONS = Set.of();

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
    Set<String> permissions = new HashSet<>();
    permissions.add(Operations.READ.getValue());

    return permissions;
  }

  @Override
  public String getName() {
    return "ReadOnlyAuthorizationService";
  }

}
