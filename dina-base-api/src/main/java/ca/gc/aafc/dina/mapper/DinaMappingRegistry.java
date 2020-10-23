package ca.gc.aafc.dina.mapper;

import ca.gc.aafc.dina.dto.RelatedEntity;
import ca.gc.aafc.dina.repository.meta.JsonApiExternalRelation;
import io.crnk.core.resource.annotations.JsonApiId;
import io.crnk.core.resource.annotations.JsonApiRelation;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Getter
public class DinaMappingRegistry {

  private final Map<Class<?>, Set<String>> resourceFieldsPerClass;
  private final Map<Class<?>, Set<String>> entityFieldsPerClass;
  private final Map<String, String> externalNameToTypeMap;
  private final Map<String, Class<?>> relationNamePerClass;
  private final Map<Class<?>, String> jsonIdFieldNamePerClass;
  private final Set<String> collectionBasedRelations;

  public DinaMappingRegistry(@NonNull Class<?> resourceClass) {
    this.resourceFieldsPerClass =
      parseFieldsPerClass(resourceClass, new HashMap<>(), DinaMappingRegistry::isNotMappable);
    this.entityFieldsPerClass = parseFieldsPerEntity();
    this.relationNamePerClass = parseMappableRelations(resourceClass);
    this.collectionBasedRelations = parseCollectionBasedRelations(resourceClass);
    this.externalNameToTypeMap = parseExternalRelationNamesToType(resourceClass);
    this.jsonIdFieldNamePerClass = parseJsonIds(resourceClass, new HashMap<>());
  }

  private Map<Class<?>, String> parseJsonIds(Class<?> cls, Map<Class<?>, String> map) {
    if (map.containsKey(cls)) {
      return map;
    }
    for (Field field : FieldUtils.getAllFieldsList(cls)) {
      if (field.isAnnotationPresent(JsonApiId.class)) {
        map.put(cls, field.getName());
      }
    }
    for (Field field : FieldUtils.getFieldsListWithAnnotation(cls, JsonApiRelation.class)) {
      Class<?> type = DinaMapper.isCollection(field.getType()) ?
        DinaMapper.getGenericType(field.getDeclaringClass(), field.getName()) : field.getType();
      parseJsonIds(type, map);
    }
    return map;
  }

  private Map<String, String> parseExternalRelationNamesToType(Class<?> resourceClass) {
    return FieldUtils.getFieldsListWithAnnotation(resourceClass, JsonApiExternalRelation.class)
      .stream()
      .collect(Collectors.toMap(
        Field::getName, field -> field.getAnnotation(JsonApiExternalRelation.class).type()));
  }

  private static Set<String> parseCollectionBasedRelations(Class<?> resourceClass) {
    return FieldUtils.getFieldsListWithAnnotation(resourceClass, JsonApiRelation.class)
      .stream()
      .filter(field -> DinaMapper.isCollection(field.getType()))
      .map(Field::getName)
      .collect(Collectors.toSet());
  }

  private static Map<String, Class<?>> parseMappableRelations(Class<?> resourceClass) {
    return FieldUtils.getFieldsListWithAnnotation(resourceClass, JsonApiRelation.class).stream()
      .filter(field -> !field.isAnnotationPresent(JsonApiExternalRelation.class))
      .collect(Collectors.toMap(
        Field::getName,
        field -> DinaMapper.isCollection(field.getType()) ?
          DinaMapper.getGenericType(field.getDeclaringClass(), field.getName()) : field.getType()));
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
   * Returns a Dto's related entity (Marked with {@link RelatedEntity}) or else null.
   *
   * @param <T>   - Class type
   * @param clazz - Class with a related entity.
   * @return a Dto's related entity, or else null
   */
  private static <T> RelatedEntity parseRelatedEntity(Class<T> clazz) {
    return clazz.getAnnotation(RelatedEntity.class);
  }

}
