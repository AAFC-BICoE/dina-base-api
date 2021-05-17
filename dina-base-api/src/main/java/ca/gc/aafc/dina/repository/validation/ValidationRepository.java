package ca.gc.aafc.dina.repository.validation;

import ca.gc.aafc.dina.dto.ValidationDto;
import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.mapper.DinaMapper;
import ca.gc.aafc.dina.mapper.DinaMappingRegistry;
import ca.gc.aafc.dina.service.DinaService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.crnk.core.engine.document.ResourceIdentifier;
import io.crnk.core.exception.BadRequestException;
import io.crnk.core.exception.MethodNotAllowedException;
import io.crnk.core.queryspec.QuerySpec;
import io.crnk.core.repository.ResourceRepositoryBase;
import io.crnk.core.resource.list.ResourceList;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@ConditionalOnProperty(value = "dina.validationEndpoint.enabled", havingValue = "true")
public class ValidationRepository extends ResourceRepositoryBase<ValidationDto, String> {

  public static final String ATTRIBUTES_KEY = "attributes";
  public static final String RELATIONSHIPS_KEY = "relationships";
  public static final String DATA_KEY = "data";
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
      DinaService<? extends DinaEntity> validatorForType = validationConfiguration.getServiceForType(type);
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
    final Class<?> resourceClass = validationConfiguration.getResourceClassForType(type);
    final Set<String> relationNames = findRelationNames(registry, resourceClass);

    final DinaEntity entity = validationConfiguration.getEntityClassForType(type)
      .getConstructor().newInstance();
    final Object dto = crnkMapper.treeToValue(data.get(ATTRIBUTES_KEY), resourceClass);

    setRelations(data, dto, relationNames);
    mapper.applyDtoToEntity(dto, entity, registry.getAttributesPerClass(), relationNames);
    entity.setUuid(UUID.randomUUID()); // Random id to avoid validating generated value.

    validationConfiguration.getServiceForType(type).validate(entity);

    // Crnk requires a created resource to have an ID. Create one here if the client did not provide one.
    resource.setId(Optional.ofNullable(resource.getId()).orElse("N/A"));
    return resource;
  }

  private void setRelations(JsonNode data, Object dto, Set<String> relationNames) {
    if (!isBlank(data) && data.has(RELATIONSHIPS_KEY) && !isBlank(data.get(RELATIONSHIPS_KEY))) {
      JsonNode relations = data.get(RELATIONSHIPS_KEY);
      if (relations.isObject()) {
        ObjectNode toObjNode = relations.deepCopy();
        toObjNode.fields().forEachRemaining(relation -> setRelation(dto, relationNames, relation));
      } else {
        throw new BadRequestException("You did not submit a valid relationship block");
      }
    }
  }

  @SneakyThrows
  private void setRelation(Object dto, Set<String> relationNames, Map.Entry<String, JsonNode> relation) {
    String relationFieldName = relation.getKey();
    if (StringUtils.isNotBlank(relationFieldName)
      && relationNames.stream().anyMatch(relationFieldName::equalsIgnoreCase)) {
      Optional<Object> newRelationInstance = newRelationInstance(relation.getValue());
      if (newRelationInstance.isPresent()) {
        PropertyUtils.setProperty(dto, relationFieldName, newRelationInstance.get());
      } else {
        PropertyUtils.setProperty(dto, relationFieldName, null);
      }
    } else {
      throw new BadRequestException(
        "A relation with field name: " + relationFieldName + " does not exist for class: "
          + dto.getClass().getSimpleName());
    }
  }

  @SneakyThrows
  private Optional<Object> newRelationInstance(JsonNode relationNode) {
    if (!isBlank(relationNode) && relationNode.has(DATA_KEY) && !isBlank(relationNode.get(DATA_KEY))) {
      JsonNode idNode = relationNode.get(DATA_KEY);
      ResourceIdentifier relationIdentifier = crnkMapper.treeToValue(idNode, ResourceIdentifier.class);
      Object relationInstance = validationConfiguration
        .getResourceClassForType(relationIdentifier.getType())
        .getConstructor()
        .newInstance();
      return Optional.of(relationInstance);
    }
    return Optional.empty();
  }

  private void validateIncomingRequest(String type, JsonNode data) {
    if (StringUtils.isBlank(type) || !validationConfiguration.getTypes().contains(type)) {
      throw new BadRequestException("You must submit a valid configuration type");
    }

    if (isBlank(data) || !data.has(ATTRIBUTES_KEY) || isBlank(data.get(ATTRIBUTES_KEY))) {
      throw new BadRequestException("You must submit a valid data block");
    }
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
