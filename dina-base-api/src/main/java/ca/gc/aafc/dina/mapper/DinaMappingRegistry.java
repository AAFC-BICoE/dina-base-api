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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DinaMappingRegistry {

  // Tracks Attributes per class for bean mapping
  @Getter
  private final Map<Class<?>, Set<String>> attributesPerClass;
  // Tracks the mappable relations per class
  private final Map<Class<?>, Set<InternalRelation>> mappableRelationsPerClass;
  // Tracks external relation types per field name for external relations mapping
  private final Map<String, String> externalNameToTypeMap;
  // Track Json Id field names for mapping
  private final Map<Class<?>, String> jsonIdFieldNamePerClass;

  public DinaMappingRegistry(@NonNull Class<?> resourceClass) {
    this.externalNameToTypeMap = parseExternalRelationNamesToType(resourceClass);
    this.attributesPerClass = new HashMap<>();
    this.mappableRelationsPerClass = new HashMap<>();
    this.jsonIdFieldNamePerClass = new HashMap<>();
    initTrackingMaps(resourceClass, new HashSet<>());
  }

  /**
   * Returns a set of the mappable relations for a given class.
   *
   * @param cls - class with relations
   * @return Returns a set of the mappable relations for a given class.
   * @throws IllegalArgumentException if the class is not tracked by the registry
   */
  public Set<InternalRelation> findMappableRelationsForClass(Class<?> cls) {
    if (!this.mappableRelationsPerClass.containsKey(cls)) {
      throw new IllegalArgumentException(cls.getSimpleName() + " is not tracked by the registry");
    }
    return this.mappableRelationsPerClass.get(cls);
  }

  /**
   * Returns the set of external relation field names tracked by the registry.
   *
   * @return set of external relation field names.
   */
  public Set<String> getExternalRelations() {
    return this.externalNameToTypeMap.keySet();
  }

  /**
   * Returns the {@link JsonApiExternalRelation} type of the given external relation field name if
   * tracked by the registry.
   *
   * @param relationFieldName - field name of the external relation.
   * @return type of the given external relation.
   * @throws IllegalArgumentException if the relationFieldName is not tracked by the registry
   */
  public String findExternalType(String relationFieldName) {
    if (!this.externalNameToTypeMap.containsKey(relationFieldName)) {
      throw new IllegalArgumentException(
        "external relation with name: " + relationFieldName + " is not tracked by the registry");
    }
    return this.externalNameToTypeMap.get(relationFieldName);
  }

  /**
   * Returns true if the relation with the given field name is external.
   *
   * @param relationFieldName - field name of the external relation.
   * @return Returns true if the relation with the given field name is external.
   */
  public boolean isRelationExternal(String relationFieldName) {
    return this.externalNameToTypeMap.keySet().stream()
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
    if (!this.jsonIdFieldNamePerClass.containsKey(cls)) {
      throw new IllegalArgumentException(cls.getSimpleName() + " is not tracked by the registry");
    }
    return this.jsonIdFieldNamePerClass.get(cls);
  }

  private void initTrackingMaps(Class<?> dtoClass, Set<Class<?>> visited) {
    if (visited.contains(dtoClass)) {
      return;
    }
    visited.add(dtoClass);

    List<Field> allFieldsList = FieldUtils.getAllFieldsList(dtoClass);
    List<Field> relationFields = FieldUtils.getFieldsListWithAnnotation(
      dtoClass,
      JsonApiRelation.class);

    trackJsonId(dtoClass, allFieldsList);
    RelatedEntity relatedEntity = dtoClass.getAnnotation(RelatedEntity.class);
    if (relatedEntity != null) {
      Class<?> entityType = relatedEntity.value();
      trackFieldsPerClass(dtoClass, entityType, allFieldsList, relationFields);
      trackMappableRelations(dtoClass, entityType, relationFields);
    }

    for (Field field : relationFields) {
      if (isCollection(field.getType())) {
        Class<?> genericType = getGenericType(field.getDeclaringClass(), field.getName());
        initTrackingMaps(genericType, visited);
      } else {
        initTrackingMaps(field.getType(), visited);
      }
    }
  }

  private void trackJsonId(Class<?> dtoClass, List<Field> allFieldsList) {
    for (Field field : allFieldsList) {
      if (field.isAnnotationPresent(JsonApiId.class)) {
        this.jsonIdFieldNamePerClass.put(dtoClass, field.getName());
      }
    }
  }

  private void trackFieldsPerClass(
    Class<?> dtoClass,
    Class<?> entityType,
    List<Field> allFieldsList,
    List<Field> relationFields
  ) {
    Set<String> fieldsToInclude = allFieldsList.stream()
      .filter(f -> !relationFields.contains(f) && DinaMappingRegistry.isFieldMappable(f))
      .map(Field::getName)
      .collect(Collectors.toSet());
    this.attributesPerClass.put(dtoClass, fieldsToInclude);
    this.attributesPerClass.put(entityType, fieldsToInclude);
  }

  private void trackMappableRelations(Class<?> dto, Class<?> entity, List<Field> relations) {
    Set<InternalRelation> mappableRelations = relations.stream()
      .filter(field -> isRelationMappable(dto, entity, field))
      .map(DinaMappingRegistry::mapToInternalRelation)
      .collect(Collectors.toSet());
    this.mappableRelationsPerClass.put(dto, mappableRelations);

    this.mappableRelationsPerClass.put(entity, mappableRelations.stream().map(
      ir -> InternalRelation.builder().name(ir.getName()).isCollection(ir.isCollection())
        .elementType(ir.getElementType().getAnnotation(RelatedEntity.class).value()).build()
    ).collect(Collectors.toSet()));
  }

  private static InternalRelation mapToInternalRelation(Field field) {
    if (isCollection(field.getType())) {
      Class<?> genericType = getGenericType(field.getDeclaringClass(), field.getName());
      return InternalRelation.builder()
        .name(field.getName()).isCollection(true).elementType(genericType).build();
    } else {
      return InternalRelation.builder()
        .name(field.getName()).isCollection(false).elementType(field.getType()).build();
    }
  }

  /**
   * Returns a map of external relation field names to their JsonApiExternalRelation.type for a
   * given class.
   *
   * @param resourceClass - a given class with external relations.
   * @return a map of external relation field names to their JsonApiExternalRelation.type
   */
  private static Map<String, String> parseExternalRelationNamesToType(Class<?> resourceClass) {
    return FieldUtils.getFieldsListWithAnnotation(resourceClass, JsonApiExternalRelation.class)
      .stream().collect(Collectors.toMap(
        Field::getName, field -> field.getAnnotation(JsonApiExternalRelation.class).type()));
  }

  /**
   * Returns true if the dina repo should map the given field. currently that means if the field is
   * not generated (Marked with {@link DerivedDtoField}) or final.
   *
   * @param field - field to evaluate
   * @return - true if the dina repo should not map the given field
   */
  private static boolean isFieldMappable(Field field) {
    return !field.isAnnotationPresent(DerivedDtoField.class)
           && !Modifier.isFinal(field.getModifiers())
           && !field.isSynthetic();
  }

  /**
   * Returns true if the dina repo should map the given relation. A relation should be mapped if it
   * is not external, and present in the given dto and entity classes.
   *
   * @param dto              - resource class of the relation
   * @param entity           - entity class of the relation
   * @param dtoRelationField - relation field to map
   * @return true if the dina repo should map the given relation.
   */
  private static boolean isRelationMappable(Class<?> dto, Class<?> entity, Field dtoRelationField) {
    return !dtoRelationField.isAnnotationPresent(JsonApiExternalRelation.class) &&
           Stream.of(entity.getDeclaredFields())
             .map(Field::getName)
             .anyMatch(dtoRelationField.getName()::equalsIgnoreCase) &&
           Stream.of(dto.getDeclaredFields())
             .map(Field::getName)
             .anyMatch(dtoRelationField.getName()::equalsIgnoreCase);
  }

  /**
   * Returns the class of the parameterized type at the first position of a given class's given
   * field.
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
    private final Class<?> elementType;
    private final boolean isCollection;
  }
}
