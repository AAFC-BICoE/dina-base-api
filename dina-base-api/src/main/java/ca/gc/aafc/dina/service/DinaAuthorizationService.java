package ca.gc.aafc.dina.service;

public interface DinaAuthorizationService {

  void authorizeCreate(Object entity);

  void authorizeUpdate(Object entity);

  void authorizeDelete(Object entity);
    
}
