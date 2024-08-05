package ca.gc.aafc.dina.repository.validation;

import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.mapper.DinaMappingRegistry;
import ca.gc.aafc.dina.repository.DinaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.crnk.core.resource.annotations.JsonApiResource;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;
import java.util.Set;

/**
 * ValidationResourceHandler can validate a json node representation of it's assigned resource type.
 *
 * @param <D> resource type
 */
// CHECKSTYLE:OFF NoFinalizer
// CHECKSTYLE:OFF SuperFinalize
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

  /**
   * Validates a given json node representation of handlers assigned resource type. Validation is run against
   * the assigned resources repository.validate() method.
   *
   * @param node       json node representation to validate
   * @param crnkMapper Object Mapper use to map Json to Objects
   */
  @SneakyThrows
  public void validate(@NonNull JsonNode node, @NonNull ObjectMapper crnkMapper) {
    if (ValidationNodeHelper.isInvalidDataBlock(node)) {
      throw new IllegalArgumentException("You must submit a valid data block");
    }

    D dto = crnkMapper.treeToValue(node.get(ValidationNodeHelper.ATTRIBUTES_KEY), resourceClass);
    setRelations(node, dto, this.mappingRegistry.findMappableRelationsForClass(resourceClass));
    dinaRepo.validate(dto);
  }

  /**
   * Returns true if the given type name is supported by this handler.
   *
   * @param typeName type name to evaluate
   * @return true if the given type name is supported by this handler.
   */
  public boolean isSupported(String typeName) {
    if (StringUtils.isBlank(typeName)) {
      return false;
    }
    return this.typeName.equalsIgnoreCase(typeName);
  }

  private void setRelations(
    JsonNode data,
    Object dto,
    Set<DinaMappingRegistry.InternalRelation> mappableRelationsForClass
  ) {
    boolean hasValidRelationBlock = !ValidationNodeHelper.isBlank(data) && data.has(ValidationNodeHelper.RELATIONSHIPS_KEY)
      && !ValidationNodeHelper.isBlank(data.get(ValidationNodeHelper.RELATIONSHIPS_KEY));
    if (hasValidRelationBlock) {
      JsonNode relations = data.get(ValidationNodeHelper.RELATIONSHIPS_KEY);
      if (relations.isObject()) {
        relations.deepCopy().fields().forEachRemaining(relation -> {
          String relationFieldName = relation.getKey();
          if (StringUtils.isNotBlank(relationFieldName)) {
            findInternalRelation(mappableRelationsForClass, relationFieldName).ifPresent(internalRelation ->
              setRelation(dto, relationFieldName, internalRelation.getDtoType()));
          }
        });
      }
    }
  }

  @SneakyThrows
  private static void setRelation(Object dto, String relationName, Class<?> resourceType) {
    PropertyUtils.setProperty(dto, relationName, resourceType.getConstructor().newInstance());
  }

  private static Optional<DinaMappingRegistry.InternalRelation> findInternalRelation(
    Set<DinaMappingRegistry.InternalRelation> relations,
    String relationFieldName
  ) {
    if (StringUtils.isBlank(relationFieldName) || CollectionUtils.isEmpty(relations)) {
      return Optional.empty();
    }
    return relations.stream().filter(ir -> ir.getName().equalsIgnoreCase(relationFieldName))
      .findFirst();
  }

  // Avoid CT_CONSTRUCTOR_THROW
  protected final void finalize() {
    // no-op
  }
}
