package ca.gc.aafc.dina.repository.validation;

import java.util.Optional;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Repository;

import ca.gc.aafc.dina.dto.ValidationDto;
import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.mapper.DinaMapper;
import ca.gc.aafc.dina.mapper.DinaMappingLayer;
import ca.gc.aafc.dina.mapper.DinaMappingRegistry;
import io.crnk.core.exception.MethodNotAllowedException;
import io.crnk.core.queryspec.QuerySpec;
import io.crnk.core.repository.ResourceRepositoryBase;
import io.crnk.core.resource.list.ResourceList;
import lombok.SneakyThrows;

@Repository
public class ValidationRepository<D, E extends DinaEntity> extends ResourceRepositoryBase<ValidationDto, String> {

  @Inject
  private ValidationResourceConfiguration<D, E> validationResourceConfiguration;

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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
  public <S extends ValidationDto> S create(S resource) {
    String type = resource.getType();
    Class<D> resourceClass = validationResourceConfiguration.getResourceClassForType(type);
    Class<E> entityClass = validationResourceConfiguration.getEntityClassForType(type);
    E entity = entityClass.getConstructor().newInstance();
    DinaMappingLayer<D,E> mappingLayer = new DinaMappingLayer<>(
      resourceClass,
      new DinaMapper<>(resourceClass), 
      validationResourceConfiguration.getServiceForType(type),
      new DinaMappingRegistry(resourceClass));
    String json = OBJECT_MAPPER.writeValueAsString(resource.getData());
    JsonNode jNode = OBJECT_MAPPER.readTree(json).get("data").get("attributes");
    D dto = OBJECT_MAPPER.treeToValue(jNode, resourceClass);
    mappingLayer.mapToEntity(dto, entity);

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
