package ca.gc.aafc.dina.repository.validation;

import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.mapper.DinaMappingRegistry;
import ca.gc.aafc.dina.repository.DinaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.crnk.core.exception.BadRequestException;
import io.crnk.core.resource.annotations.JsonApiResource;
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

  public ValidationResourceHandler(Class<D> resourceClass, DinaRepository<D, ? extends DinaEntity> dinaRepo) {
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
  public void validate(JsonNode node, ObjectMapper crnkMapper) {
    D dto = crnkMapper.treeToValue(node.get(ValidationRepository.ATTRIBUTES_KEY), resourceClass);
    setRelations(node, dto, findRelationNames(this.mappingRegistry, resourceClass));
    dinaRepo.validate(dto);
  }

  public boolean isSupported(String typeName) {
    return this.typeName.equalsIgnoreCase(typeName);
  }

  private void setRelations(JsonNode data, Object dto, Set<String> relationNames) {
    if (!ValidationRepository.isBlank(data) && data.has(ValidationRepository.RELATIONSHIPS_KEY) && !ValidationRepository
      .isBlank(data.get(ValidationRepository.RELATIONSHIPS_KEY))) {
      JsonNode relations = data.get(ValidationRepository.RELATIONSHIPS_KEY);
      if (relations.isObject()) {
        ObjectNode toObjNode = relations.deepCopy();
        toObjNode.fields().forEachRemaining(relation -> setRelation(dto, relationNames, relation));
      } else {
        throw new BadRequestException("You did not submit a valid relationship block");
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

  @SneakyThrows
  private void setRelation(Object dto, Set<String> relationNames, Map.Entry<String, JsonNode> relation) {
    String relationFieldName = relation.getKey();
    if (StringUtils.isNotBlank(relationFieldName) &&
      relationNames.stream().anyMatch(relationFieldName::equalsIgnoreCase)) {
      this.mappingRegistry.findMappableRelationsForClass(this.resourceClass)
        .stream()
        .findFirst()
        .ifPresent(internalRelation -> setRelationObj(dto, relationFieldName, internalRelation));
    } else {
      throw new BadRequestException(
        "A relation with field name: " + relationFieldName + " does not exist for class: "
          + dto.getClass().getSimpleName());
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
