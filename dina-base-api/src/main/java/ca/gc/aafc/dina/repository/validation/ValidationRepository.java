package ca.gc.aafc.dina.repository.validation;

import ca.gc.aafc.dina.dto.ValidationDto;
import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.mapper.DinaMapper;
import ca.gc.aafc.dina.mapper.DinaMappingRegistry;
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
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

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
  private final ValidationRegistry validationRegistry;
  private final ObjectMapper crnkMapper;

  public ValidationRepository(
    @NonNull ValidationResourceConfiguration validationResourceConfiguration,
    @NonNull ObjectMapper crnkMapper
  ) {
    super(ValidationDto.class);
    this.crnkMapper = crnkMapper;
    this.validationRegistry = new ValidationRegistry(validationResourceConfiguration);
  }

  @Override
  @SneakyThrows
  public <S extends ValidationDto> S create(S resource) {
    final String type = resource.getType();
    final JsonNode data = resource.getData();
    validateIncomingRequest(type, data);

    final ValidationRegistry.ValidationEntry validationEntry = validationRegistry.getEntryForType(type)
      .orElseThrow(ValidationRepository::getInvalidTypeException);

    @SuppressWarnings("unchecked") // Mapper is cast for type compliance from wildcard ? to object
    final DinaMapper<Object, DinaEntity> mapper = (DinaMapper<Object, DinaEntity>) validationEntry.getMapper();
    final DinaMappingRegistry registry = validationEntry.getMappingRegistry();
    final Class<?> resourceClass = validationEntry.getResourceClass();
    final Set<String> relationNames = findRelationNames(registry, resourceClass);

    final DinaEntity entity = validationEntry.getEntityClass().getConstructor().newInstance();
    final Object dto = crnkMapper.treeToValue(data.get(ATTRIBUTES_KEY), resourceClass);

    setRelations(data, dto, relationNames);
    mapper.applyDtoToEntity(dto, entity, registry.getAttributesPerClass(), relationNames);

    validationEntry.getDinaService().validate(entity);

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
      Object relationInstance = validationRegistry.getEntryForType(relationIdentifier.getType())
        .orElseThrow(ValidationRepository::getInvalidTypeException)
        .getResourceClass()
        .getConstructor()
        .newInstance();
      return Optional.of(relationInstance);
    }
    return Optional.empty();
  }

  private void validateIncomingRequest(String type, JsonNode data) {
    if (StringUtils.isBlank(type) || !validationRegistry.hasEntryForType(type)) {
      throw getInvalidTypeException();
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

  private static BadRequestException getInvalidTypeException() {
    return new BadRequestException("You must submit a valid configuration type");
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
