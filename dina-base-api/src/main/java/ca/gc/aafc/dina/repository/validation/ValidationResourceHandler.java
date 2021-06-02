package ca.gc.aafc.dina.repository.validation;

import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.mapper.DinaMappingRegistry;
import ca.gc.aafc.dina.repository.DinaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.crnk.core.resource.annotations.JsonApiResource;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ValidationResourceHandler<D> {
  private final DinaMappingRegistry mappingRegistry;
  private final Class<D> resourceClass;
  private final DinaRepository<D, ? extends DinaEntity> dinaRepo;
  private final String typeName;

  public ValidationResourceHandler(
    @NonNull Class<D> resourceClass,
    @NonNull DinaRepository<D, ? extends DinaEntity> dinaRepo
  ) {
    this.resourceClass = resourceClass;

    JsonApiResource jsonApiResource = resourceClass.getAnnotation(JsonApiResource.class);
    if (jsonApiResource == null || StringUtils.isBlank(jsonApiResource.type())) {
      throw new IllegalArgumentException(
        "The provided resource must have a valid JsonApiResource annotation");
    }

    this.typeName = jsonApiResource.type();
    this.dinaRepo = dinaRepo;
    this.mappingRegistry = new DinaMappingRegistry(resourceClass);
  }

  @SneakyThrows
  public void validate(@NonNull JsonNode node, @NonNull ObjectMapper crnkMapper) {
    if (ValidationNodeHelper.isInvalidDataBlock(node)) {
      throw new IllegalArgumentException("You must submit a valid data block");
    }

    D dto = crnkMapper.treeToValue(node.get(ValidationNodeHelper.ATTRIBUTES_KEY), resourceClass);
    setRelations(node, dto, findRelationNames(this.mappingRegistry, resourceClass));
    dinaRepo.validate(dto);
  }

  public boolean isSupported(String typeName) {
    if (StringUtils.isBlank(typeName)) {
      return false;
    }
    return this.typeName.equalsIgnoreCase(typeName);
  }

  private void setRelations(JsonNode data, Object dto, Set<String> relationNames) {
    boolean hasValidRelationBlock = !ValidationNodeHelper.isBlank(data)
      && data.has(ValidationNodeHelper.RELATIONSHIPS_KEY)
      && !ValidationNodeHelper.isBlank(data.get(ValidationNodeHelper.RELATIONSHIPS_KEY));
    if (hasValidRelationBlock) {
      JsonNode relations = data.get(ValidationNodeHelper.RELATIONSHIPS_KEY);
      if (relations.isObject()) {
        ObjectNode toObjNode = relations.deepCopy();
        toObjNode.fields().forEachRemaining(relation -> setRelation(dto, relationNames, relation));
      }
    }
  }

  private static Set<String> findRelationNames(DinaMappingRegistry registry, Class<?> aClass) {
    return registry
      .findMappableRelationsForClass(aClass)
      .stream()
      .map(DinaMappingRegistry.InternalRelation::getName)
      .collect(Collectors.toSet());
  }

  private void setRelation(Object dto, Set<String> relationNames, Map.Entry<String, JsonNode> relation) {
    String relationFieldName = relation.getKey();
    if (StringUtils.isNotBlank(relationFieldName) &&
      relationNames.stream().anyMatch(relationFieldName::equalsIgnoreCase)) {
      this.mappingRegistry.findMappableRelationsForClass(this.resourceClass)
        .stream()
        .findFirst()
        .ifPresent(internalRelation -> setRelationObj(dto, relationFieldName, internalRelation));
    }
  }

  @SneakyThrows
  private void setRelationObj(
    Object dto,
    String relationFieldName,
    DinaMappingRegistry.InternalRelation internalRelation
  ) {
    PropertyUtils.setProperty(
      dto, relationFieldName, internalRelation.getElementType().getConstructor().newInstance());
  }

}
