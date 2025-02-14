package ca.gc.aafc.dina.security.auth;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class DinaAdminCUDAuthorizationService extends PermissionAuthorizationService {

  @Override
  @PreAuthorize("hasAdminRole(@currentUser, 'DINA_ADMIN')")
  public void authorizeCreate(Object entity) {

  }

  @Override
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
    return "DinaAdminCUDAuthorizationService";
  }

}
