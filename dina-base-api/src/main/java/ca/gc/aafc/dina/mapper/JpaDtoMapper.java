package ca.gc.aafc.dina.mapper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Streams;

import ca.gc.aafc.dina.repository.SelectionHandler;
import ca.gc.aafc.dina.util.TriConsumer;
import io.crnk.core.engine.information.resource.ResourceField;
import io.crnk.core.engine.information.resource.ResourceInformation;
import io.crnk.core.engine.internal.utils.PropertyUtils;
import io.crnk.core.engine.registry.ResourceRegistry;
import io.crnk.core.queryspec.IncludeFieldSpec;
import io.crnk.core.queryspec.IncludeRelationSpec;
import io.crnk.core.queryspec.QuerySpec;
import lombok.NonNull;
import lombok.SneakyThrows;

/**
 * Maps DTOs to JPA entities.
 * @deprecated will be removed with JPA repository classes
 */
@Deprecated(forRemoval = true, since = "0.42")
public class JpaDtoMapper {
  
  private final BiMap<Class<?>, Class<?>> jpaEntities;
  private final Map<Class<?>, List<CustomFieldResolverSpec<?>>> customFieldResolvers;

  private final ExpressionParser entityParser = new SpelExpressionParser(
    // Auto-grow must be false for the entity to avoid adding empty related entities:
    new SpelParserConfiguration(false, false)
  );

  private final ExpressionParser dtoParser = new SpelExpressionParser(
    // Auto-grow must be true for the DTO instantiate related DTOs:
    new SpelParserConfiguration(true, true)
  );
  
  public JpaDtoMapper (
    @NonNull Map<Class<?>, Class<?>> jpaEntities,
    Map<Class<?>, List<CustomFieldResolverSpec<?>>> customFieldResolvers
  ) {
    this.jpaEntities = HashBiMap.create(jpaEntities);
    this.customFieldResolvers = customFieldResolvers;
  }
  
  public Class<?> getEntityClassForDto(Class<?> dtoClass) {
    return jpaEntities.get(dtoClass);
  }
  
  public Class<?> getDtoClassForEntity(Class<?> entityClass) {
    return jpaEntities.inverse().get(entityClass);
  }
  

  
  /**
   * Converts an Entity to a DTO based on the selected fields and includes in the QuerySpec.
   * 
   * @param entity
   * @param querySpec
   * @param resourceRegistry
   * @return the DTO
   */
  public Object toDto(Object entity, QuerySpec querySpec, ResourceRegistry resourceRegistry) {
    Class<?> dtoClass = this.getDtoClassForEntity(entity.getClass());

    Map<Class<?>, Set<String>> selectedFieldsPerClass = getSelectedFieldsPerClass(resourceRegistry, querySpec);
    Set<String> rootSelectedFields = selectedFieldsPerClass.get(dtoClass);

    Object dto = toSingleDto(entity, dtoClass, rootSelectedFields);

    StandardEvaluationContext entityContext = new StandardEvaluationContext(entity);
    StandardEvaluationContext dtoContext = new StandardEvaluationContext(dto);

    for (IncludeRelationSpec relation : querySpec.getIncludedRelations()) {
      String pathString = String.join(".", relation.getAttributePath());
      Class<?> relationDtoClass = getPropertyClass(dtoClass, relation.getAttributePath());

      Object relationEntity;
      try {
        relationEntity = entityParser.parseExpression(pathString).getValue(entityContext);
      } catch (EvaluationException ee) {
        relationEntity = null;
      }

      if (relationEntity != null) {
        Set<String> relationSelectedFields = selectedFieldsPerClass.get(relationDtoClass);
        Object relationDto = toSingleDto(relationEntity, relationDtoClass, relationSelectedFields);
        dtoParser.parseExpression(pathString).setValue(dtoContext, relationDto);
      }
    }

    return dto;
  }

  /**
   * Apply the changed data held in a DTO object to a JPA entity.
   *
   * @param dto
   * @param entity
   * @param resourceRegistry
   * @param findFct function used to find an entity based on the exposed identifier
   */
  public void applyDtoToEntity(Object dto, Object entity, ResourceRegistry resourceRegistry, BiFunction<Serializable, Class<?>,Object> findFct) {
    ResourceInformation resourceInformation = resourceRegistry.findEntry(dto.getClass())
        .getResourceInformation();

    Set<String> attributes = resourceInformation.getAttributeFields()
        .stream()
        .map(af -> af.getUnderlyingName())
        .collect(Collectors.toSet());
    // Apply the DTO's attribute values to the entity.
    applyDtoAttributesToEntity(dto, entity, attributes);

    // Apply the DTO's relation values to the entity.
    List<ResourceField> relationFields = resourceInformation.getRelationshipFields();
    for (ResourceField relationField : relationFields) {
      String relationName = relationField.getUnderlyingName();

      // Get the ResourceInformation of the related element type.
      ResourceInformation relatedResourceInformation = resourceRegistry
          .findEntry(relationField.getElementType()).getResourceInformation();

      List<Serializable> targetIds = null;

      // Handle a to-many relation field.
      if (relationField.isCollection()) {
        // Get the collection of DTOs that specify the new collection elements.
        @SuppressWarnings("unchecked")
        Collection<Object> relatedResourceDtos = (Collection<Object>) PropertyUtils.getProperty(dto,
            relationName);

        // If the DTO collection is not null, this means that a new collection has been specified by
        // the client.
        if (relatedResourceDtos != null) {
          // Get the IDs of the collection elements.
          targetIds = relatedResourceDtos.stream()
              .map(relatedDto -> (Serializable) relatedResourceInformation.getId(relatedDto))
              .collect(Collectors.toList());
        }

        // Handle a to-one relation field.
      } else {
        Object relatedResourceDto = PropertyUtils.getProperty(dto, relationName);
        if (relatedResourceDto != null) {
          targetIds = Collections
              .singletonList((Serializable) relatedResourceInformation.getId(relatedResourceDto));
        } else {
          targetIds = new ArrayList<>();
        }
      }

      // Only modify relations if targetIds is specified. targetIds could be an empty list,
      // which would set a to-one relation to null. targetIds being null means that no change is
      // made.
      if (targetIds != null) {
        this.modifyRelation(entity, targetIds, relationName, findFct, (sourceCollection, targetEntities) -> {
          sourceCollection.clear();
          sourceCollection.addAll(targetEntities);
        }, Collection::add, (targetEntity, oppositeFieldName, sourceEntity) -> PropertyUtils
            .setProperty(targetEntity, oppositeFieldName, sourceEntity), resourceRegistry);
      }

    }
  }

  /**
   * Apply the given attributes of a DTO to an Entity.
   * 
   * @param dto        - dto to map
   * @param entity     - entity to map too
   * @param attributes - attributes to map
   */
  private void applyDtoAttributesToEntity(Object dto, Object entity, Set<String> attributes) {

    // Apply the DTO's attribute values to the entity.
    for (String attributeName : attributes) {

      // Skip read-only derived fields and fields with custom resolvers:
      if (isGenerated(dto.getClass(), attributeName) ||
          hasCustomFieldResolver(entity.getClass(), attributeName)) {
        continue;
      }

      PropertyUtils.setProperty(entity, attributeName, PropertyUtils.getProperty(dto, attributeName));
    }

    // Apply the DTO's attribute values using custom resolvers.
    consumeFieldResolvers(entity.getClass(), cfr -> {
      if (attributes.contains(cfr.getField())) {
        PropertyUtils.setProperty(entity, cfr.getField(), cfr.getResolver().apply(dto));
      }
    });
  }

  /**
   * Modifies a relation
   * 
   * @param sourceEntity
   *          The source entity in the relation.
   * @param targetIds
   *          The IDs of the target entities to add/remove to the relation.
   * @param fieldName
   *          The name of the relation field on the source entity.
   @param findFct
   *          Function used to find an entity based on the exposed identifier
   * @param handleSourceCollectionAndTargetEntities
   *          When the source entity's relation field is a collection, how to handle the target
   *          entities (e.g. add or remove them to the collection).
   * @param handleOppositeCollectionAndSourceEntity
   *          When the target entity's relation field is a collection, how to handle the source
   *          entity (e.g. add or remove it to the collection).
   * @param handleTargetEntityAndFieldNameAndSourceEntity
   *          When the target entity's relation field is a singular reference, how to handle the
   *          source entity (e.g. set the target's field to reference the source entity).
   * @param resourceRegistry
   *          the Crnk ResourceRegistry
   */
  public void modifyRelation(@NonNull Object sourceEntity,
      @NonNull Iterable<Serializable> targetIds, @NonNull String fieldName, BiFunction<Serializable, Class<?>,Object> findFct,
      BiConsumer<Collection<Object>, Collection<Object>> handleSourceCollectionAndTargetEntities,
      BiConsumer<Collection<Object>, Object> handleOppositeCollectionAndSourceEntity,
      TriConsumer<Object, String, Object> handleTargetEntityAndFieldNameAndSourceEntity,
      ResourceRegistry resourceRegistry) {

    Class<?> entityClass = sourceEntity.getClass();
    Class<?> dtoClass = this.getDtoClassForEntity(entityClass);

    // Get the target resource DTO class
    Class<? extends Object> targetResourceClass = resourceRegistry.findEntry(dtoClass)
        .getResourceInformation().findRelationshipFieldByName(fieldName).getElementType();

    Collection<Object> targetEntities = Streams.stream(targetIds)
        .map(id -> findFct.apply(id, this.getEntityClassForDto(targetResourceClass)))
        .collect(Collectors.toList());

    // Get the current value of the source object's relation field.
    Object sourceFieldValue = PropertyUtils.getProperty(sourceEntity, fieldName);

    // Handle a to-many or to-one relation.
    if (sourceFieldValue instanceof Collection) {
      @SuppressWarnings("unchecked")
      Collection<Object> sourceCollection = (Collection<Object>) sourceFieldValue;
      handleSourceCollectionAndTargetEntities.accept((Collection<Object>) sourceCollection,
          targetEntities);
    } else {
      handleTargetEntityAndFieldNameAndSourceEntity.accept(sourceEntity, fieldName,
          targetEntities.iterator().hasNext() ? targetEntities.iterator().next() : null);
    }

    // In case of a bidirectional relation, get the opposite relation field name.
    String oppositeFieldName = resourceRegistry.findEntry(dtoClass).getResourceInformation()
        .findFieldByName(fieldName).getOppositeName();

    if (oppositeFieldName != null) {
      ResourceField oppositeField = resourceRegistry.findEntry(targetResourceClass)
          .getResourceInformation().findFieldByName(oppositeFieldName);

      // Handle to-many or to-one relation from the opposite end of the bidirectional relation.
      if (oppositeField.isCollection()) {
        @SuppressWarnings("unchecked")
        List<Collection<Object>> oppositeCollections = targetEntities.stream()
            .map(targetEntity -> (Collection<Object>) PropertyUtils.getProperty(targetEntity,
                oppositeFieldName))
            .collect(Collectors.toList());

        for (Collection<Object> oppositeCollection : oppositeCollections) {
          if (!oppositeCollection.contains(sourceEntity)) {
            handleOppositeCollectionAndSourceEntity.accept(oppositeCollection, sourceEntity);
          }
        }
      } else {
        for (Object targetEntity : targetEntities) {
          handleTargetEntityAndFieldNameAndSourceEntity.accept(targetEntity, oppositeFieldName,
              sourceEntity);
        }
      }
    }
  }

  /** Whether a dto field is generated and read-only. */
  @SneakyThrows(NoSuchFieldException.class)
  private boolean isGenerated(Class<?> dtoClass, String field) {
    return dtoClass.getDeclaredField(field).isAnnotationPresent(DerivedDtoField.class);
  }

  /**
   * Returns true if the given class has custom field resolvers for a given field.
   * 
   * @param clazz - class with resolvers
   * @param field - field with r resolver
   * @return - true if the given class has custom field resolvers for a given
   *         field.
   */
  private boolean hasCustomFieldResolver(Class<?> clazz, String field) {
    return !customFieldResolvers.getOrDefault(clazz, new ArrayList<>())
        .stream()
        .filter(cfr->StringUtils.equalsIgnoreCase(field, cfr.getField()))
        .collect(Collectors.toList()).isEmpty();
  }

  /**
   * Converts an Entity to a DTO, only including the fields on this DTO, not included DTOs.
   */
  @SneakyThrows
  private Object toSingleDto(Object entity, Class<?> dtoClass, Set<String> selectedFields) {
    Object dto = dtoClass.getConstructor().newInstance();

    StandardEvaluationContext entityContext = new StandardEvaluationContext(entity);
    StandardEvaluationContext dtoContext = new StandardEvaluationContext(dto);

    for (String field : selectedFields) {
      Object value;
      try {
        value = entityParser.parseExpression(field).getValue(entityContext);
      } catch (EvaluationException ee) {
        value = null;
      }
      if (value != null && !hasCustomFieldResolver(dtoClass, field)) {
        dtoParser.parseExpression(field).setValue(dtoContext, value);
      }
    }

    // Apply custom field resolvers:
    consumeFieldResolvers(dtoClass, cfr -> {
      if (selectedFields.contains(cfr.getField())) {
        dtoParser.parseExpression(cfr.getField()).setValue(dtoContext, cfr.getResolver().apply(entity));
      }
    });

    return dto;
  }

  /**
   * Gets the selected fields as attribute paths from the querySpec.
   * 
   * @param querySpec
   * @param root
   * @return
   */
  private Map<Class<?>, Set<String>> getSelectedFieldsPerClass(
      ResourceRegistry resourceRegistry, QuerySpec querySpec) {
    Map<Class<?>, Set<String>> selectedFields = new HashMap<>();
    
    Set<String> selectedFieldsOfThisClass = new HashSet<>();
    ResourceInformation resourceInformation = resourceRegistry
        .getEntry(querySpec.getResourceClass())
        .getResourceInformation();
    
    // If no fields are specified, include all fields.
    if (querySpec.getIncludedFields().size() == 0) {
      // Add the attribute fields
      selectedFieldsOfThisClass.addAll(
          resourceInformation.getAttributeFields()
              .stream()
              .map(ResourceField::getUnderlyingName)
              .collect(Collectors.toList())
      );
      // Add the ID fields for to-one relationships.
      selectedFieldsOfThisClass.addAll(
          resourceInformation.getRelationshipFields()
              .stream()
              .filter(field -> !field.isCollection())
              // Map each ResourceField to the attribute path of the related resource's ID, e.g.
              // "region.id".
              .map(field -> {
                // Add the field name to the attribute path e.g. "region"
                String prefix = field.getUnderlyingName();
                // Add the ID field name to the attribute path e.g. "id"
                String suffix = resourceRegistry.findEntry(field.getElementType())
                    .getResourceInformation()
                    .getIdField()
                    .getUnderlyingName();
                // Return the attribute path, e.g. "region.id".
                return prefix + "." + suffix;
              })
              .collect(Collectors.toList())
      );
    } else {
      for (IncludeFieldSpec includedField : querySpec.getIncludedFields()) {
        selectedFieldsOfThisClass.add(String.join(".", includedField.getAttributePath()));
      }
    }

    // The id field is always selected, even if not explicitly requested by the user.
    String idAttribute = SelectionHandler.getIdAttribute(
      querySpec.getResourceClass(),
      resourceRegistry
    );
    if (!selectedFieldsOfThisClass.contains(idAttribute)) {
      selectedFieldsOfThisClass.add(idAttribute);
    }
    
    selectedFields.put(querySpec.getResourceClass(), selectedFieldsOfThisClass);
    
    // Add the selected fields for includes where sparse fields are requested.
    for (QuerySpec nestedSpec : querySpec.getNestedSpecs()) {
      selectedFields.putAll(this.getSelectedFieldsPerClass(resourceRegistry, nestedSpec));
    }
    
    // Add the selected fields for includes where fields are not selected for that type.
    for (IncludeRelationSpec rel : querySpec.getIncludedRelations()) {
      Class<?> relationClass = this.getPropertyClass(
          querySpec.getResourceClass(),
          rel.getAttributePath()
      );
      if (!selectedFields.containsKey(relationClass)) {
        selectedFields
            .putAll(this.getSelectedFieldsPerClass(resourceRegistry, new QuerySpec(relationClass)));
      }
    }
    
    return selectedFields;
  }

  /**
   * Get the class of a property that may be more than one element away.
   * 
   * @param baseType
   * @param attributePath
   * @return
   */
  private Class<?> getPropertyClass(Class<?> baseType, List<String> attributePath) {
    Class<?> type = baseType;
    for (String pathElement : attributePath) {
      type = PropertyUtils.getPropertyClass(type, pathElement);
    }
    return type;
  }

  /**
   * Consume the field resolvers of a given class with a given consumer.
   * 
   * @param clazz    - class with field resolvers.
   * @param consumer - consumer to consume resolvers.
   */
  private void consumeFieldResolvers(Class<?> clazz, Consumer<CustomFieldResolverSpec> consumer) {
    List<CustomFieldResolverSpec<?>> resolverSpecs = customFieldResolvers.get(clazz);
    if (resolverSpecs != null) {
      for (CustomFieldResolverSpec<?> spec : resolverSpecs) {
        consumer.accept(spec);
      }
    }
  }

}
