package ca.gc.aafc.dina.security;

/**
 * Authorization service that allows all operations.
 */
public class AllowAllAuthorizationService implements DinaAuthorizationService {

  @Override
  public void authorizeCreate(Object entity) {
    // do nothing
  }

  @Override
  public void authorizeUpdate(Object entity) {
    // do nothing
  }

  @Override
  public void authorizeDelete(Object entity) {
    // do nothing
  }

}
