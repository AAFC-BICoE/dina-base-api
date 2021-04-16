package ca.gc.aafc.dina.repository.validation;

import org.springframework.stereotype.Repository;

import ca.gc.aafc.dina.dto.ValidationDto;
import ca.gc.aafc.dina.entity.DinaEntity;

import io.crnk.core.exception.MethodNotAllowedException;
import io.crnk.core.queryspec.QuerySpec;
import io.crnk.core.repository.ResourceRepositoryBase;
import io.crnk.core.resource.list.ResourceList;

@Repository
public class ValidationRepository<E extends DinaEntity> extends ResourceRepositoryBase<ValidationDto, String> {

  //private final DefaultDinaService<? extends DinaEntity> service;

  private final ValidationResourceConfiguration validationResourceConfiguration;

  protected ValidationRepository(ValidationResourceConfiguration validationResourceConfiguration) {
      super(ValidationDto.class);
      this.validationResourceConfiguration = validationResourceConfiguration;
  }

/**
   * Throws {@link MethodNotAllowedException}
   * Create not allowed on read-only
   * @param resource
   * @param <S>
   * @return
   */
  @Override
  public <S extends ValidationDto> S create(S resource) {
    validationResourceConfiguration.getServiceForType(resource.getType())
      .validate((E) resource.getData().get("data"));
    
    return null;
  }

  /**
   * Throws {@link MethodNotAllowedException}
   * Save not allowed on validation
   * @param resource
   * @param <S>
   * @return
   */
  @Override
  public <S extends ValidationDto> S save(S resource) {
    throw new MethodNotAllowedException("PUT/PATCH");
  }

  /**
   * Throws {@link MethodNotAllowedException}
   * Delete not allowed on validation
   * @param id
   */
  @Override
  public void delete(String id) {
    throw new MethodNotAllowedException("DELETE");
  }

  @Override
  public ResourceList<ValidationDto> findAll(QuerySpec arg0) {
    // TODO Auto-generated method 
    return null;
  }
 
}
