package ca.gc.aafc.dina.security;

import java.util.Set;

/**
 * To be used with the {@link ca.gc.aafc.dina.repository.ReadOnlyDinaRepository} to return a correct set of
 * permissions. Does not authorize and will return a set of empty permissions.
 */
public class ReadOnlyAuthorizationService implements DinaAuthorizationService {

  public static final Set<String> NO_PERMISSIONS = Set.of();

  @Override
  public void authorizeCreate(Object entity) {
  }

  @Override
  public void authorizeUpdate(Object entity) {
  }

  @Override
  public void authorizeDelete(Object entity) {
  }

  @Override
  public Set<String> getPermissionsForObject(Object target) {
    return NO_PERMISSIONS;
  }

}
