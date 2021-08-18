package ca.gc.aafc.dina.mapper;

import ca.gc.aafc.dina.dto.RelatedEntity;
import ca.gc.aafc.dina.repository.meta.JsonApiExternalRelation;
import io.crnk.core.resource.annotations.JsonApiId;
import io.crnk.core.resource.annotations.JsonApiRelation;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Registry to track information regarding a given resource class. Useful to obtain certain meta information
 * regarding the domain of resource.
 */
public class DinaMappingRegistry {

  // Tracks Attributes per class for bean mapping
  @Getter
  private final Map<Class<?>, Set<String>> attributesPerClass;
  // Track Field adapters per class
  private final Map<Class<?>, DinaResourceEntry> resourceGraph;

  /**
   * Parsing a given resource graph requires the use of reflection. A DinaMappingRegistry should not be
   * constructed in a repetitive manner where performance is needed.
   *
   * @param resourceClass - resource class to track
   */
  public DinaMappingRegistry(@NonNull Class<?> resourceClass) {
    resourceGraph = initGraph(resourceClass, new HashSet<>());
    this.attributesPerClass = this.resourceGraph.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry->entry.getValue()
      .getAttributeNames()));
  }

  private Map<Class<?>, DinaResourceEntry> initGraph(Class<?> resourceClass, HashSet<Class<?>> visited) {
    HashMap<Class<?>, DinaResourceEntry> graph = new HashMap<>();

    if (visited.contains(resourceClass)) {
      return graph;
    }
    visited.add(resourceClass);

    RelatedEntity relatedEntity = resourceClass.getAnnotation(RelatedEntity.class);
    if (relatedEntity != null) {
      Class<?> entityClass = relatedEntity.value();
      Field[] dtoClassDeclaredFields = resourceClass.getDeclaredFields();

      Set<String> attributes = new HashSet<>();
      Set<InternalRelation> internalRelations = new HashSet<>();

      for (Field field : dtoClassDeclaredFields) {
        if (isMappableRelation(resourceClass, entityClass, field)) {
          internalRelations.add(mapToInternalRelation(field));
          graph.putAll(initGraph(parseGenericTypeForField(field), visited));
        } else if (isFieldValidAttribute(resourceClass, entityClass, field)) {
          if (fieldHasSameDataType(resourceClass, entityClass, field.getName())) {
            attributes.add(field.getName());
          } else {
            if (!isRelatedEntityPresentForField(field)) {
              throwDataTypeMismatchException(resourceClass, entityClass, field.getName());
            } else {
              internalRelations.add(mapToInternalRelation(field));
              graph.putAll(initGraph(parseGenericTypeForField(field), visited));
            }
          }
        }
      }

      DinaResourceEntry resourceEntry = DinaResourceEntry.builder()
        .dtoClass(resourceClass)
        .entityClass(entityClass)
        .externalNameToTypeMap(parseExternalRelationNamesToType(resourceClass))
        .attributeNames(attributes)
        .internalRelations(internalRelations)
        .jsonIdFieldName(parseJsonIdFieldName(resourceClass))
        .fieldAdapterHandler(new DinaFieldAdapterHandler<>(resourceClass))
        .build();
      graph.put(resourceClass, resourceEntry);
      graph.put(entityClass, resourceEntry);
    }
    return graph;
  }

  private String parseJsonIdFieldName(Class<?> resourceClass) {
    for (Field field : FieldUtils.getAllFieldsList(resourceClass)) {
      if (field.isAnnotationPresent(JsonApiId.class)) {
        return field.getName();
      }
    }
    return null;
  }

  private static void throwDataTypeMismatchException(Class<?> dto, Class<?> entity, String attrib) {
    throw new IllegalStateException("data type for Field:{" + attrib + "} on DTO:{" + dto.getSimpleName()
      + "} does not match the field from Entity:{" + entity.getSimpleName() + "}");
  }

  /**
   * Returns a set of the mappable relations for a given class.
   *
   * @param cls - class with relations
   * @return Returns a set of the mappable relations for a given class.
   * @throws IllegalArgumentException if the class is not tracked by the registry
   */
  public Set<InternalRelation> findMappableRelationsForClass(Class<?> cls) {
    checkClassTracked(cls);
    return this.resourceGraph.get(cls).getInternalRelations();
  }

  /**
   * Returns the set of external relation field names tracked by the registry.
   *
   * @return set of external relation field names.
   */
  public Set<String> getExternalRelations(Class<?> cls) {
    checkClassTracked(cls);
    return this.resourceGraph.get(cls).getExternalNameToTypeMap().keySet();
  }

  /**
   * Returns the {@link JsonApiExternalRelation} type of the given external relation field name if tracked by
   * the registry.
   *
   * @param relationFieldName - field name of the external relation.
   * @return type of the given external relation.
   * @throws IllegalArgumentException if the relationFieldName is not tracked by the registry
   */
  public String findExternalType(Class<?> cls , String relationFieldName) {
    checkClassTracked(cls);
    if (!this.resourceGraph.get(cls).getExternalNameToTypeMap().containsKey(relationFieldName)) {
      throw new IllegalArgumentException(
        "external relation with name: " + relationFieldName + " is not tracked by the registry");
    }
    return  this.resourceGraph.get(cls).getExternalNameToTypeMap().get(relationFieldName);
  }

  /**
   * Returns true if the relation with the given field name is external.
   *
   * @param relationFieldName - field name of the external relation.
   * @return Returns true if the relation with the given field name is external.
   */
  public boolean isRelationExternal(Class<?> cls , String relationFieldName) {
    checkClassTracked(cls);
    return this.resourceGraph.get(cls).getExternalNameToTypeMap().keySet().stream()
      .anyMatch(relationFieldName::equalsIgnoreCase);
  }

  /**
   * Returns the json id field name of a given class.
   *
   * @param cls - cls with json id field
   * @return the json id field name of a given class.
   * @throws IllegalArgumentException if the class is not tracked by the registry
   */
  public String findJsonIdFieldName(Class<?> cls) {
    checkClassTracked(cls);
    return this.resourceGraph.get(cls).getJsonIdFieldName();
  }

  private void checkClassTracked(Class<?> cls) {
    if (!this.resourceGraph.containsKey(cls)) {
      throw new IllegalArgumentException(cls.getSimpleName() + " is not tracked by the registry");
    }
  }

  public Optional<DinaFieldAdapterHandler<?>> findFieldAdapterForClass(Class<?> cls) {
    if (!this.resourceGraph.containsKey(cls)) {
      return Optional.empty();
    }
    return Optional.ofNullable(this.resourceGraph.get(cls).getFieldAdapterHandler());
  }

  public Set<DinaFieldAdapterHandler<?>> getFieldAdapters() {
    return this.resourceGraph.values()
      .stream()
      .map(DinaResourceEntry::getFieldAdapterHandler)
      .collect(Collectors.toSet());
  }

  /**
   * Returns the nested resource type from a given base resource type and attribute path. The deepest nested
   * resource that can be resolved will be returned. The original resource is returned if a nested resource is
   * not present in the attribute path, or the resources are not tracked by the registry.
   *
   * @param resource      - base resource to traverse
   * @param attributePath - attribute path to follow
   * @return - the nested resource type from a given path.
   */
  public Class<?> resolveNestedResourceFromPath(
    @NonNull Class<?> resource,
    @NonNull List<String> attributePath
  ) {
    Class<?> nested = resource;
    for (String attribute : attributePath) {
      Optional<InternalRelation> relation = this.findMappableRelationsForClass(nested).stream()
        .filter(internalRelation -> internalRelation.getName().equalsIgnoreCase(attribute))
        .findAny();
      if (relation.isPresent()) {
        nested = relation.get().getDtoType();
      } else {
        break;
      }
    }
    return nested;
  }


  private static InternalRelation mapToInternalRelation(Field field) {
    Class<?> fieldType = field.getType();
    if (isCollection(fieldType)) {
      Class<?> genericType = getGenericType(field.getDeclaringClass(), field.getName());
      return InternalRelation.builder()
        .name(field.getName())
        .isCollection(true)
        .dtoType(genericType)
        .entityType(genericType.getAnnotation(RelatedEntity.class).value())
        .build();
    } else {
      return InternalRelation.builder()
        .name(field.getName())
        .isCollection(false)
        .dtoType(fieldType)
        .entityType(fieldType.getAnnotation(RelatedEntity.class).value())
        .build();
    }
  }

  /**
   * Returns a map of external relation field names to their JsonApiExternalRelation.type for a given class.
   *
   * @param resourceClass - a given class with external relations.
   * @return a map of external relation field names to their JsonApiExternalRelation.type
   */
  private static Map<String, String> parseExternalRelationNamesToType(Class<?> resourceClass) {
    return Map.copyOf(
      FieldUtils.getFieldsListWithAnnotation(resourceClass, JsonApiExternalRelation.class)
        .stream().collect(Collectors.toMap(
          Field::getName,
          field -> field.getAnnotation(JsonApiExternalRelation.class).type())));
  }

  /**
   * Returns true if the dina repo should map the given field as an attribute. An attribute in this context is
   * a field that has its value mapped directly.
   *
   * @param dtoClass    dto class to validate against
   * @param entityClass entity class to validate against
   * @param field       field to evaluate
   * @return - true if the dina repo should not map the given field
   */
  private static boolean isFieldValidAttribute(Class<?> dtoClass, Class<?> entityClass, Field field) {
    return !field.isAnnotationPresent(IgnoreDinaMapping.class)
      && !field.isAnnotationPresent(JsonApiRelation.class)
      && fieldExistsInBothClasses(dtoClass, entityClass, field.getName())
      && !Modifier.isFinal(field.getModifiers())
      && !field.isSynthetic();
  }

  /**
   * Returns true if the dina repo should map the given relation. A relation should be mapped if it is not
   * external, and present in the given dto and entity classes.
   *
   * @param dto              - resource class of the relation
   * @param entity           - entity class of the relation
   * @param dtoRelationField - relation field to map
   * @return true if the dina repo should map the given relation.
   */
  private static boolean isMappableRelation(Class<?> dto, Class<?> entity, Field dtoRelationField) {
    return dtoRelationField.isAnnotationPresent(JsonApiRelation.class)
      && !dtoRelationField.isAnnotationPresent(IgnoreDinaMapping.class)
      && !dtoRelationField.isAnnotationPresent(JsonApiExternalRelation.class)
      && fieldExistsInBothClasses(dto, entity, dtoRelationField.getName());
  }

  private static boolean fieldExistsInBothClasses(Class<?> dtoClass, Class<?> entityClass, String fieldName) {
    return Stream.of(dtoClass.getDeclaredFields()).anyMatch(field -> fieldName.equals(field.getName()))
      && Stream.of(entityClass.getDeclaredFields()).anyMatch(field -> fieldName.equals(field.getName()));
  }

  @SneakyThrows
  private static boolean fieldHasSameDataType(Class<?> dtoClass, Class<?> entityClass, String fieldName) {
    Type typeOnDto = dtoClass.getDeclaredField(fieldName).getGenericType();
    Type typeOnEntity = entityClass.getDeclaredField(fieldName).getGenericType();

    if (!typeOnEntity.equals(typeOnDto)) { // Data types must match!
      return false;
    }

    if (typeOnDto instanceof ParameterizedType) { // If parameterized generic type must match
      return getGenericType(dtoClass, fieldName).equals(getGenericType(entityClass, fieldName));
    }
    return true;
  }

  /**
   * Returns the class of the parameterized type at the first position of a given class's given field.
   * <p>
   * given class is assumed to be a {@link ParameterizedType}
   *
   * @param source    given class
   * @param fieldName field name of the given class to parse
   * @return class of the paramterized type at the first position
   */
  @SneakyThrows
  private static Class<?> getGenericType(Class<?> source, String fieldName) {
    ParameterizedType genericType = (ParameterizedType) source
      .getDeclaredField(fieldName)
      .getGenericType();
    return (Class<?>) genericType.getActualTypeArguments()[0];
  }

  private static boolean isRelatedEntityPresentForField(Field field) {
    return parseGenericTypeForField(field).isAnnotationPresent(RelatedEntity.class);
  }

  private static Class<?> parseGenericTypeForField(Field field) {
    if (field.getGenericType() instanceof ParameterizedType) {
      return (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
    } else {
      return field.getType();
    }
  }

  /**
   * Returns true if the given class is a collection
   *
   * @param clazz - class to check
   * @return true if the given class is a collection
   */
  private static boolean isCollection(Class<?> clazz) {
    return Collection.class.isAssignableFrom(clazz);
  }

  /**
   * Internal Relation Representing a field of class to be mapped
   */
  @Builder
  @Getter
  public static class InternalRelation {
    private final String name;
    private final Class<?> dtoType;
    private final Class<?> entityType;
    private final boolean isCollection;
  }

  @Builder
  @Getter
  public static class DinaResourceEntry {
    private Class<?> dtoClass;
    private Class<?> entityClass;
    private String jsonIdFieldName;
    private Set<String> attributeNames;
    private Set<InternalRelation> internalRelations;
    private Map<String, String> externalNameToTypeMap;
    private DinaFieldAdapterHandler<?> fieldAdapterHandler;
  }
}
