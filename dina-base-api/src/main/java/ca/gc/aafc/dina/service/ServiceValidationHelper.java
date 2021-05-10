package ca.gc.aafc.dina.service;

import ca.gc.aafc.dina.entity.DinaEntity;

public class ServiceValidationHelper {

  protected static <E extends DinaEntity> void callValidatedCreate(DefaultDinaService<E> dinaService, E entity) {
    dinaService.validatedCreate(entity);
  }

  protected static <E extends DinaEntity> E callValidatedUpdate(DefaultDinaService<E> dinaService, E entity) {
    return dinaService.validatedUpdate(entity);
  }
}
