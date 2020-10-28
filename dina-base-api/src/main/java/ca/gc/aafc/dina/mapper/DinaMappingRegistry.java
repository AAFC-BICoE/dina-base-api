package ca.gc.aafc.dina.mapper;

import ca.gc.aafc.dina.dto.RelatedEntity;
import ca.gc.aafc.dina.repository.meta.JsonApiExternalRelation;
import io.crnk.core.resource.annotations.JsonApiId;
import io.crnk.core.resource.annotations.JsonApiRelation;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DinaMappingRegistry {

  // Tracks the resource graph for bean mapping
  @Getter
  private final Map<Class<?>, Set<String>> resourceFieldsPerClass;
  // Tracks the entity graph for bean mapping
  @Getter
  private final Map<Class<?>, Set<String>> entityFieldsPerClass;
  // Tracks the relation types per mappable relation
  @Getter
  private final Map<String, Class<?>> relationTypesPerMappableRelation;
  // Tracks the mappable relations per class
  private final Map<Class<?>, Set<String>> mappableRelationsPerClass;
  // Tracks external relation types per field name for external relations mapping
  private final Map<String, String> externalNameToTypeMap;
  // Tracks the name of relations which are collections
  private final Map<Class<?>, Set<String>> collectionBasedRelationsPerClass;
  // Track Json Id field names for mapping
  private final Map<Class<?>, String> jsonIdFieldNamePerClass;

  public DinaMappingRegistry(@NonNull Class<?> resourceClass) {
    this.resourceFieldsPerClass =
      parseFieldsPerClass(resourceClass, new HashMap<>(), DinaMappingRegistry::isNotMappable);
    this.entityFieldsPerClass = parseFieldsPerEntity();
    this.mappableRelationsPerClass = new HashMap<>();
    this.relationTypesPerMappableRelation = parseRelationTypesPerRelation(resourceClass);
    this.collectionBasedRelationsPerClass = parseCollectionBasedRelations(
      resourceClass, new HashMap<>(), new HashSet<>());
    this.externalNameToTypeMap = parseExternalRelationNamesToType(resourceClass);
    this.jsonIdFieldNamePerClass = new HashMap<>();
    parseGraph(resourceClass, new HashSet<>());
  }

  /**
   * Returns a set of the mappable relations for a given class, or null if a given class is not
   * tracked.
   *
   * @param cls - class with relations
   * @return Returns a set of the mappable relations for a given class.
   */
  public Set<String> findMappableRelationsForClass(Class<?> cls) {
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
   * Returns the type of the given external relation if tracked by the registry otherwise null.
   *
   * @param relationFieldName - field name of the external relation.
   * @return type of the given external relation.
   */
  public String findExternalType(String relationFieldName) {
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
   * Returns true if the given classes given relation field name is of a Java collection type.
   *
   * @param cls               - class to check
   * @param relationFieldName - field name to check
   * @return true if the given classes given relation field name is of a Java collection type.
   */
  public boolean isRelationCollection(Class<?> cls, String relationFieldName) {
    return this.collectionBasedRelationsPerClass.containsKey(cls) &&
           this.collectionBasedRelationsPerClass.get(cls)
             .stream().anyMatch(relationFieldName::equalsIgnoreCase);
  }

  /**
   * Returns the json id field name of a given class.
   *
   * @param cls - cls with json id field
   * @return the json id field name of a given class.
   */
  public String findJsonIdFieldName(Class<?> cls) {
    return this.jsonIdFieldNamePerClass.get(cls);
  }

  /**
   * Returns the resolved type of a fieldname for a given source. If the type is a collection, the
   * first generic type is returned.
   *
   * @param source    - source object of the field
   * @param fieldName - field name
   * @return Field type or the first genric type if the field is a collection
   */
  @SneakyThrows
  public static Class<?> getResolvedType(Object source, String fieldName) {
    Class<?> propertyType = PropertyUtils.getPropertyType(source, fieldName);
    return isCollection(propertyType) ? getGenericType(source.getClass(), fieldName) : propertyType;
  }

  private void parseGraph(Class<?> cls, Set<Class<?>> visited) {
    if (visited.contains(cls)) {
      return;
    }
    visited.add(cls);

    //json ids
    for (Field field : FieldUtils.getAllFieldsList(cls)) {
      if (field.isAnnotationPresent(JsonApiId.class)) {
        this.jsonIdFieldNamePerClass.put(cls, field.getName());
      }
    }

    RelatedEntity relatedEntity = cls.getAnnotation(RelatedEntity.class);

    List<Field> relationFields = FieldUtils.getFieldsListWithAnnotation(cls, JsonApiRelation.class);
    if (relatedEntity != null) {
      Class<?> entityType = relatedEntity.value();

      //mappable relations
      Set<String> mappableRelations = relationFields.stream()
        .filter(field ->
          !field.isAnnotationPresent(JsonApiExternalRelation.class) &&
          Stream.of(entityType.getDeclaredFields())
            .map(Field::getName)
            .anyMatch(field.getName()::equalsIgnoreCase))
        .map(Field::getName)
        .collect(Collectors.toSet());
      this.mappableRelationsPerClass.put(cls, mappableRelations);
      this.mappableRelationsPerClass.put(entityType, mappableRelations);
    }

    for (Field field : relationFields) {
      if (isCollection(field.getType())) {
        Class<?> genericType = getGenericType(field.getDeclaringClass(), field.getName());
        parseGraph(genericType, visited);
      } else {
        parseGraph(field.getType(), visited);
      }
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
      .stream()
      .collect(Collectors.toMap(
        Field::getName, field -> field.getAnnotation(JsonApiExternalRelation.class).type()));
  }

  private static Map<Class<?>, Set<String>> parseCollectionBasedRelations(
    Class<?> cls,
    Map<Class<?>, Set<String>> map,
    Set<Class<?>> visited
  ) {
    if (visited.contains(cls)) {
      return map;
    }
    visited.add(cls);
    RelatedEntity relatedEntity = cls.getAnnotation(RelatedEntity.class);
    if (relatedEntity != null) {
      for (Field field : FieldUtils.getFieldsListWithAnnotation(cls, JsonApiRelation.class)) {
        if (isCollection(field.getType())) {

          Set<String> resourceRelations = map.getOrDefault(cls, new HashSet<>());
          Set<String> relatedEntityRelations = map.getOrDefault(
            relatedEntity.value(), new HashSet<>());

          resourceRelations.add(field.getName());
          relatedEntityRelations.add(field.getName());

          map.put(cls, resourceRelations);
          map.put(relatedEntity.value(), relatedEntityRelations);

          Class<?> genericType = getGenericType(field.getDeclaringClass(), field.getName());
          parseCollectionBasedRelations(genericType, map, visited);
        } else {
          parseCollectionBasedRelations(field.getType(), map, visited);
        }
      }
    }
    return map;
  }

  /**
   * Returns a map of relation types per relation field name that are not external relations and are
   * eligible for bean mapping and relation linking to a db backed source. relations that are
   * collections will be mapped to their collections generic type.
   *
   * @param resourceClass - a given class with relations.
   * @return a map of relation field names per class that are not external relations
   */
  private static Map<String, Class<?>> parseRelationTypesPerRelation(Class<?> resourceClass) {
    return FieldUtils.getFieldsListWithAnnotation(resourceClass, JsonApiRelation.class).stream()
      .filter(field -> !field.isAnnotationPresent(JsonApiExternalRelation.class))
      .collect(Collectors.toMap(
        Field::getName,
        field -> isCollection(field.getType()) ?
          getGenericType(field.getDeclaringClass(), field.getName()) : field.getType()));
  }

  /**
   * Transverses a given class to return a map of fields per class parsed from the given class. Used
   * to determine the necessary classes and fields per class when mapping a java bean. Fields marked
   * with {@link JsonApiRelation} will be treated as separate classes to map and will be transversed
   * and mapped.
   *
   * @param <T>            - Type of class
   * @param clazz          - Class to parse
   * @param fieldsPerClass - initial map to use
   * @param ignoreIf       - predicate to return true for fields to be removed
   * @return a map of fields per class
   */
  @SneakyThrows
  private static <T> Map<Class<?>, Set<String>> parseFieldsPerClass(
    @NonNull Class<T> clazz,
    @NonNull Map<Class<?>, Set<String>> fieldsPerClass,
    @NonNull Predicate<Field> ignoreIf
  ) {
    if (fieldsPerClass.containsKey(clazz)) {
      return fieldsPerClass;
    }

    List<Field> relationFields = FieldUtils.getFieldsListWithAnnotation(
      clazz,
      JsonApiRelation.class
    );

    List<Field> attributeFields = FieldUtils.getAllFieldsList(clazz).stream()
      .filter(f -> !relationFields.contains(f) && !f.isSynthetic() && !ignoreIf.test(f))
      .collect(Collectors.toList());

    Set<String> fieldsToInclude = attributeFields.stream()
      .map(Field::getName)
      .collect(Collectors.toSet());

    fieldsPerClass.put(clazz, fieldsToInclude);

    parseRelations(clazz, fieldsPerClass, relationFields, ignoreIf);

    return fieldsPerClass;
  }

  /**
   * Helper method to parse the fields of a given list of relations and add them to a given map.
   *
   * @param <T>            - Type of class
   * @param clazz          - class containing the relations
   * @param fieldsPerClass - map to add to
   * @param relationFields - relation fields to transverse
   */
  @SneakyThrows
  private static <T> void parseRelations(
    Class<T> clazz,
    Map<Class<?>, Set<String>> fieldsPerClass,
    List<Field> relationFields,
    Predicate<Field> removeIf
  ) {
    for (Field relationField : relationFields) {
      if (Collection.class.isAssignableFrom(relationField.getType())) {
        ParameterizedType genericType = (ParameterizedType) clazz
          .getDeclaredField(relationField.getName())
          .getGenericType();
        for (Type elementType : genericType.getActualTypeArguments()) {
          parseFieldsPerClass((Class<?>) elementType, fieldsPerClass, removeIf);
        }
      } else {
        parseFieldsPerClass(relationField.getType(), fieldsPerClass, removeIf);
      }
    }
  }

  /**
   * Returns a map of fields per entity class.
   *
   * @return a map of fields per entity class.
   */
  private Map<Class<?>, Set<String>> parseFieldsPerEntity() {
    return resourceFieldsPerClass.entrySet()
      .stream()
      .filter(e -> parseRelatedEntity(e.getKey()) != null)
      .map(e -> new AbstractMap.SimpleEntry<>(parseRelatedEntity(e.getKey()).value(), e.getValue()))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  /**
   * Returns the related entity of a dto (Marked with {@link RelatedEntity}) or else null.
   *
   * @param <T>   - Class type
   * @param clazz - Class with a related entity.
   * @return the related entity of a dto, or else null
   */
  private static <T> RelatedEntity parseRelatedEntity(Class<T> clazz) {
    return clazz.getAnnotation(RelatedEntity.class);
  }

  /**
   * Returns true if the dina repo should not map the given field. currently that means if the field
   * is generated (Marked with {@link DerivedDtoField}) or final.
   *
   * @param field - field to evaluate
   * @return - true if the dina repo should not map the given field
   */
  private static boolean isNotMappable(Field field) {
    return field.isAnnotationPresent(DerivedDtoField.class) ||
           Modifier.isFinal(field.getModifiers());
  }

  /**
   * Returns the class of the paramterized type at the first position of a given class's given
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

}
