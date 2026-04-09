package ca.gc.aafc.dina.security.auth;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * Required DINA_ADMIN role for all operations
 */
@Service
public class DinaAdminAuthorizationService extends PermissionAuthorizationService {

  /**
   * Utility method since the entity is not used to check the role
   */
  @PreAuthorize("hasAdminRole(@currentUser, 'DINA_ADMIN')")
  public void authorize() {

  }

  @Override
  @PreAuthorize("hasAdminRole(@currentUser, 'DINA_ADMIN')")
  public void authorizeCreate(Object entity) {

  }

  @Override
  @PreAuthorize("hasAdminRole(@currentUser, 'DINA_ADMIN')")
  public void authorizeRead(Object entity) {

  }

  @Override
  @PreAuthorize("hasAdminRole(@currentUser, 'DINA_ADMIN')")
  public void authorizeUpdate(Object entity) {

  }

  @Override
  @PreAuthorize("hasAdminRole(@currentUser, 'DINA_ADMIN')")
  public void authorizeDelete(Object entity) {

  }

  @Override
  public String getName() {
    return "DinaAdminAuthorizationService";
  }

}
