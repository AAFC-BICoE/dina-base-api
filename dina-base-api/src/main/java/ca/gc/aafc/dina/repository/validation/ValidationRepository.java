package ca.gc.aafc.dina.repository.validation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Repository;

import ca.gc.aafc.dina.dto.ValidationDto;
import ca.gc.aafc.dina.entity.DinaEntity;
import io.crnk.core.engine.internal.utils.PropertyUtils;
import io.crnk.core.exception.MethodNotAllowedException;
import io.crnk.core.queryspec.QuerySpec;
import io.crnk.core.repository.ResourceRepositoryBase;
import io.crnk.core.resource.list.ResourceList;
import lombok.SneakyThrows;

@Repository
public class ValidationRepository<E extends DinaEntity> extends ResourceRepositoryBase<ValidationDto, String> {

  @Inject
  private ValidationResourceConfiguration<E> validationResourceConfiguration;

  protected ValidationRepository() {
    super(ValidationDto.class);
  }

/**
   * Throws {@link MethodNotAllowedException}
   * Create not allowed on read-only
   * @param resource
   * @param <S>
   * @return
   */
  @Override
  @SneakyThrows
  @SuppressWarnings("unchecked")
  public <S extends ValidationDto> S create(S resource) {
    String type = resource.getType();

    Class<E> entityClass = validationResourceConfiguration.getEntityClassForType(type);
    E entity = entityClass.getConstructor().newInstance();

    for(Object o : ((LinkedHashMap<String, Object>)((LinkedHashMap<String, Object>) resource.getData().get("data")).get("attributes")).entrySet()) {
      Map.Entry<String, Object> entry = (Map.Entry<String, Object>) o;
      String key = entry.getKey();
      PropertyUtils.setProperty(entity, key, entry.getValue());
    }

   validationResourceConfiguration.getServiceForType(resource.getType())
       .validate(entity);
       
    // Crnk requires a created resource to have an ID. Create one here if the client did not provide one.
    resource.setId(Optional.ofNullable(resource.getId()).orElse("N/A"));
    
    return resource;
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
