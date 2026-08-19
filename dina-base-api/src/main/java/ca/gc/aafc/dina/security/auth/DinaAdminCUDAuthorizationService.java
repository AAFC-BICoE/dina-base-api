package ca.gc.aafc.dina.security.auth;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class DinaAdminCUDAuthorizationService extends PermissionAuthorizationService {

  @Override
  @PreAuthorize("isAdmin(@currentUser)")
  public void authorizeCreate(Object entity) {

  }

  @Override
  @PreAuthorize("allow()")
  public void authorizeRead(Object entity) {

  }

  @Override
  @PreAuthorize("isAdmin(@currentUser)")
  public void authorizeUpdate(Object entity) {

  }

  @Override
  @PreAuthorize("isAdmin(@currentUser)")
  public void authorizeDelete(Object entity) {

  }

  @Override
  public String getName() {
    return DinaAdminCUDAuthorizationService.class.getSimpleName();
  }

}
