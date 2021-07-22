package ca.gc.aafc.dina.security;

import java.util.Set;

public interface DinaAuthorizationService {

  void authorizeCreate(Object entity);

  void authorizeUpdate(Object entity);

  void authorizeDelete(Object entity);

  Set<String> getPermissionsForObject(Object target);

  String getName();

}
