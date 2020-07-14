package ca.gc.aafc.dina.security;

import java.io.Serializable;

import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;

import ca.gc.aafc.dina.entity.DinaEntity;

public class GroupBasedPermissionEvaluator implements PermissionEvaluator {

  @Override
  public boolean hasPermission(Authentication auth, Object targetDomainObject, Object permission) {
    if ((auth == null) || (targetDomainObject == null) || !(permission instanceof String)
        || !(targetDomainObject instanceof DinaEntity)) {
      return false;
    }

    DinaEntity entity = (DinaEntity) targetDomainObject;

    return false;
  }

  @Override
  public boolean hasPermission(Authentication auth, Serializable targetId, String targetType,
      Object permission) {
    if ((auth == null) || (targetType == null) || !(permission instanceof String)) {
      return false;
    }
    // TODO Auto-generated method stub
    return false;
  }

}
