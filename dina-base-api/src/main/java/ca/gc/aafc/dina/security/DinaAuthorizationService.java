package ca.gc.aafc.dina.security;

public interface DinaAuthorizationService {

    void authorizeCreate(Object entity);

    void authorizeUpdate(Object entity);

    void authorizeDelete(Object entity);
    
}
