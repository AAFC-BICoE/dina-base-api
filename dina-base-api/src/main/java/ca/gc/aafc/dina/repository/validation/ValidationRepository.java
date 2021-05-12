package ca.gc.aafc.dina.repository.validation;

import ca.gc.aafc.dina.dto.ValidationDto;
import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.mapper.DinaMapper;
import ca.gc.aafc.dina.mapper.DinaMappingRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.crnk.core.exception.BadRequestException;
import io.crnk.core.exception.MethodNotAllowedException;
import io.crnk.core.queryspec.QuerySpec;
import io.crnk.core.repository.ResourceRepositoryBase;
import io.crnk.core.resource.list.ResourceList;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;
import org.springframework.validation.Validator;

import javax.validation.ValidationException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
@ConditionalOnProperty(value = "dina.validationEndpoint.enabled", havingValue = "true")
public class ValidationRepository extends ResourceRepositoryBase<ValidationDto, String> {

  public static final String ATTRIBUTES_KEY = "attributes";
  private final ValidationResourceConfiguration validationConfiguration;
  private final ObjectMapper crnkMapper;
  private final Map<String, DinaMappingRegistry> registryMap = new HashMap<>();
  private final Map<String, DinaMapper<?, DinaEntity>> dinaMapperMap = new HashMap<>();

  public ValidationRepository(
    @NonNull ValidationResourceConfiguration validationResourceConfiguration,
    @NonNull ObjectMapper crnkMapper
  ) {
    super(ValidationDto.class);
    this.validationConfiguration = validationResourceConfiguration;
    this.crnkMapper = crnkMapper;
    initMaps(validationResourceConfiguration);
  }

  private void initMaps(ValidationResourceConfiguration validationResourceConfiguration) {
    if (CollectionUtils.isEmpty(validationResourceConfiguration.getTypes())) {
      throw new IllegalStateException("The validation configuration must return a set of types, " +
        "if no types require validation consider using dina.validationEndpoint.enabled: false");
    }
    validationResourceConfiguration.getTypes().forEach(type -> {
      Class<?> resourceClass = validationResourceConfiguration.getResourceClassForType(type);
      Class<? extends DinaEntity> entityClass = validationResourceConfiguration.getEntityClassForType(type);
      Validator validatorForType = validationConfiguration.getValidatorForType(type);
      if (resourceClass == null || entityClass == null || validatorForType == null) {
        throw new IllegalStateException(
          "The validation configuration must supply a validator, resource, and entity class for the given type: " + type);
      }
      DinaMappingRegistry registry = new DinaMappingRegistry(resourceClass);
      registryMap.put(type, registry);
      dinaMapperMap.put(type, new DinaMapper<>(resourceClass, registry));
    });
  }

  @Override
  @SneakyThrows
  public <S extends ValidationDto> S create(S resource) {
    final String type = resource.getType();
    final JsonNode data = resource.getData();
    validateIncomingRequest(type, data);

    @SuppressWarnings("unchecked") // Mapper is cast for type compliance from wildcard ? to object
    final DinaMapper<Object, DinaEntity> mapper = (DinaMapper<Object, DinaEntity>) dinaMapperMap.get(type);
    final DinaMappingRegistry registry = registryMap.get(type);
    final DinaEntity entity = validationConfiguration.getEntityClassForType(type).getConstructor().newInstance();
    final Object dto = crnkMapper.treeToValue(
      data.get(ATTRIBUTES_KEY),
      validationConfiguration.getResourceClassForType(type));

    // Bean mapping
    final Set<String> relationNames = findRelationNames(registry, dto.getClass());
    mapper.applyDtoToEntity(dto, entity, registry.getAttributesPerClass(), relationNames);

    // Error validating
    final Errors errors = findValidationErrors(type, entity);
    validateErrors(errors);

    // Crnk requires a created resource to have an ID. Create one here if the client did not provide one.
    resource.setId(Optional.ofNullable(resource.getId()).orElse("N/A"));
    return resource;
  }

  private void validateIncomingRequest(String type, JsonNode data) {
    if (StringUtils.isBlank(type) || !validationConfiguration.getTypes().contains(type)) {
      throw new BadRequestException("You must submit a valid configuration type");
    }

    if (isBlank(data) || !data.has(ATTRIBUTES_KEY) || isBlank(data.get(ATTRIBUTES_KEY))) {
      throw new BadRequestException("You must submit a valid data block");
    }
  }

  private static void validateErrors(Errors errors) {
    if (errors.hasErrors()) {
      Optional<String> errorMsg = errors.getAllErrors()
        .stream()
        .map(ObjectError::getDefaultMessage)
        .findAny();

      errorMsg.ifPresent(msg -> {
        throw new ValidationException(msg);
      });
    }
  }

  private Errors findValidationErrors(String type, DinaEntity entity) {
    Errors errors = new BeanPropertyBindingResult(
      entity, entity.getUuid() != null ? entity.getUuid().toString() : "");
    validationConfiguration.getValidatorForType(type).validate(entity, errors);
    return errors;
  }

  private static Set<String> findRelationNames(DinaMappingRegistry registry, Class<?> aClass) {
    return registry
      .findMappableRelationsForClass(aClass)
      .stream()
      .map(DinaMappingRegistry.InternalRelation::getName)
      .collect(Collectors.toSet());
  }

  private static boolean isBlank(JsonNode data) {
    return data == null || data.isNull() || data.isEmpty();
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
    throw new MethodNotAllowedException("GET");
  }

}
