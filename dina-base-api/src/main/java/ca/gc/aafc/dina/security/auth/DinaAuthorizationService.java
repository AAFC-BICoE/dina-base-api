package ca.gc.aafc.dina.security.auth;

import java.util.Set;

public interface DinaAuthorizationService {

  void authorizeCreate(Object entity);

  void authorizeRead(Object entity);

  void authorizeUpdate(Object entity);

  void authorizeDelete(Object entity);

  Set<String> getPermissionsForObject(Object target);

  String getName();

}
