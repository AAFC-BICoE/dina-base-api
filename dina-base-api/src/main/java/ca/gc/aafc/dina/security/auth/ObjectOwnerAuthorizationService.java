package ca.gc.aafc.dina.security.auth;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class ObjectOwnerAuthorizationService extends PermissionAuthorizationService {

  @Override
  @PreAuthorize("hasObjectOwnership(@currentUser, #entity)")
  public void authorizeCreate(Object entity) {

  }

  @Override
  public void authorizeRead(Object entity) {

  }

  @Override
  @PreAuthorize("hasObjectOwnership(@currentUser, #entity)")
  public void authorizeUpdate(Object entity) {

  }

  @Override
  @PreAuthorize("hasObjectOwnership(@currentUser, #entity)")
  public void authorizeDelete(Object entity) {

  }

  @Override
  public String getName() {
    return "ObjectOwnerAuthorizationService";
  }
}
