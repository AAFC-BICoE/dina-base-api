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
import lombok.NonNull;
import lombok.SneakyThrows;
import org.springframework.stereotype.Repository;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;

import javax.validation.ValidationException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class ValidationRepository<D, E extends DinaEntity> extends ResourceRepositoryBase<ValidationDto, String> {

  private final ValidationResourceConfiguration<D, E> validationResourceConfiguration;
  private final ObjectMapper crnkMapper;
  private final Map<String, DinaMappingRegistry> registryMap = new HashMap<>();
  private final Map<String, DinaMapper<D, E>> dinaMapperMap = new HashMap<>();

  protected ValidationRepository(
    @NonNull ValidationResourceConfiguration<D, E> validationResourceConfiguration,
    @NonNull ObjectMapper crnkMapper
  ) {
    super(ValidationDto.class);
    this.validationResourceConfiguration = validationResourceConfiguration;
    this.crnkMapper = crnkMapper;
    validationResourceConfiguration.getTypes().forEach(type -> {
      Class<D> resourceClass = validationResourceConfiguration.getResourceClassForType(type);
      registryMap.put(type, new DinaMappingRegistry(resourceClass));
      dinaMapperMap.put(type, new DinaMapper<>(resourceClass));
    });
  }

  @Override
  @SneakyThrows
  public <S extends ValidationDto> S create(S resource) {
    String type = resource.getType();
    Class<D> resourceClass = validationResourceConfiguration.getResourceClassForType(type);
    Class<E> entityClass = validationResourceConfiguration.getEntityClassForType(type);
    E entity = entityClass.getConstructor().newInstance();

    DinaMappingRegistry registry = registryMap.get(type);
    DinaMapper<D, E> mapper = dinaMapperMap.get(type);

    JsonNode data = resource.getData();
    D dto = crnkMapper.treeToValue(data.get("data").get("attributes"), resourceClass);

    // Bean mapping
    Set<String> relationNames = findRelationNames(registry, dto.getClass());
    mapper.applyDtoToEntity(dto, entity, registry.getAttributesPerClass(), relationNames);

    Errors errors = findValidationErrors(type, entity);

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

  private Errors findValidationErrors(String type, E entity) {
    Errors errors = new BeanPropertyBindingResult(
      entity, entity.getUuid() != null ? entity.getUuid().toString() : "");
    validationResourceConfiguration.getValidatorForType(type).validate(entity, errors);
    return errors;
  }

  private static Set<String> findRelationNames(DinaMappingRegistry registry, Class<?> aClass) {
    return registry
      .findMappableRelationsForClass(aClass)
      .stream()
      .map(DinaMappingRegistry.InternalRelation::getName)
      .collect(Collectors.toSet());
  }

}
