package ca.gc.aafc.dina.security;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class DinaAdminCUDAuthorizationService extends PermissionAuthorizationService {

  @Override
  @PreAuthorize("hasDinaRole(@currentUser, 'DINA_ADMIN')")
  public void authorizeCreate(Object entity) {

  }

  @Override
  @PreAuthorize("hasMinimumDinaRole(@currentUser, 'READ_ONLY')")
  public void authorizeRead(Object entity) {

  }

  @Override
  @PreAuthorize("hasDinaRole(@currentUser, 'DINA_ADMIN')")
  public void authorizeUpdate(Object entity) {

  }

  @Override
  @PreAuthorize("hasDinaRole(@currentUser, 'DINA_ADMIN')")
  public void authorizeDelete(Object entity) {

  }

  @Override
  public String getName() {
    return "DinaAdminCUDAuthorizationService";
  }

}
