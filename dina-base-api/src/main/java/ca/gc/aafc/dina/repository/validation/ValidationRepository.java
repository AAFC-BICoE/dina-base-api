package ca.gc.aafc.dina.repository.validation;

import ca.gc.aafc.dina.dto.ValidationDto;
import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.mapper.DinaMapper;
import ca.gc.aafc.dina.mapper.DinaMappingRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.crnk.core.exception.MethodNotAllowedException;
import io.crnk.core.queryspec.QuerySpec;
import io.crnk.core.repository.ResourceRepositoryBase;
import io.crnk.core.resource.list.ResourceList;
import lombok.SneakyThrows;
import org.springframework.stereotype.Repository;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;

import javax.inject.Inject;
import javax.validation.ValidationException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class ValidationRepository<D, E extends DinaEntity> extends ResourceRepositoryBase<ValidationDto, String> {

  @Inject
  private ValidationResourceConfiguration<D, E> validationResourceConfiguration;

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  protected ValidationRepository() {
    super(ValidationDto.class);
  }

  @Override
  @SneakyThrows
  public <S extends ValidationDto> S create(S resource) {
    String type = resource.getType();
    Class<D> resourceClass = validationResourceConfiguration.getResourceClassForType(type);
    Class<E> entityClass = validationResourceConfiguration.getEntityClassForType(type);
    E entity = entityClass.getConstructor().newInstance();

    DinaMappingRegistry registry = new DinaMappingRegistry(resourceClass);
    DinaMapper<D, E> mapper = new DinaMapper<>(resourceClass);

    JsonNode data = resource.getData();
    D dto = OBJECT_MAPPER.treeToValue(data.get("data").get("attributes"), resourceClass);

    Set<DinaMappingRegistry.InternalRelation> mappableRelationsForClass = registry.findMappableRelationsForClass(
      dto.getClass());

    // Bean mapping
    Set<String> relationNames = mappableRelationsForClass.stream()
      .map(DinaMappingRegistry.InternalRelation::getName).collect(Collectors.toSet());
    mapper.applyDtoToEntity(dto, entity, registry.getAttributesPerClass(), relationNames);
    Objects.requireNonNull(entity);

    Errors errors = new BeanPropertyBindingResult(
      entity, entity.getUuid() != null ? entity.getUuid().toString() : "");
    validationResourceConfiguration.getValidatorForType(type).validate(entity, errors);

    if (errors.hasErrors()) {
      Optional<String> errorMsg = errors.getAllErrors()
        .stream()
        .map(ObjectError::getDefaultMessage)
        .findAny();

      errorMsg.ifPresent(msg -> {
        throw new ValidationException(msg);
      });
    }

    // Crnk requires a created resource to have an ID. Create one here if the client did not provide one.
    resource.setId(Optional.ofNullable(resource.getId()).orElse("N/A"));

    return resource;
  }

  @Override
  public <S extends ValidationDto> S save(S resource) {
    throw new MethodNotAllowedException("PUT/PATCH");
  }

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
