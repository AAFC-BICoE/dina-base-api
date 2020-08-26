package ca.gc.aafc.dina.mapper;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

import ca.gc.aafc.dina.dto.RelatedEntity;
import io.crnk.core.resource.annotations.JsonApiId;
import io.crnk.core.resource.annotations.JsonApiRelation;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.SneakyThrows;

/**
 * DTO to Entity Bean mapper (and vice-versa). Used to map fields between DTO's
 * and Entities.
 *
 * @param <D> - Type of Dto
 * @param <E> - Type of Entity
 */
@AllArgsConstructor
public class DinaMapper<D, E> {

  private final Class<D> dtoClass;
  private final Map<Class<?>, CustomFieldHandler<?, ?>> handlers;
  private final Map<Class<?>, Set<String>> fieldsPerClass;
  private final Map<Class<?>, Set<String>> relationPerClass;

  /**
   * <p>
   * Used to construct an instance of the dina mapper where the dto class will be
   * scanned and all custom field handlers needed to resolve the entity graph will
   * parsed from the given dto class.
   * <p>
   *
   * <p>
   * Use this constructor if you have no custom fields to resolve or you are
   * unsure if you can supply the custom field handlers per class.
   * <p>
   * 
   * @param dtoClass
   */
  public DinaMapper(@NonNull Class<D> dtoClass) {
    this(dtoClass, new HashMap<>(), new HashMap<>(), new HashMap<>());
    initMaps(dtoClass);
  }

  private void initMaps(Class<D> dtoClass) {
    Set<Class<?>> dtoClasses = parseGraph(dtoClass);

    for (Class<?> dto : dtoClasses) {
      RelatedEntity annotation = dto.getAnnotation(RelatedEntity.class);

      if (annotation != null) {
        Class<?> relatedEntity = annotation.value();
        CustomFieldHandler<?, ?> handler = new CustomFieldHandler<>(dto, relatedEntity);
        handlers.put(dto, handler);
        handlers.put(relatedEntity, handler);

        fieldsPerClass.put(dto, parseFieldNames(dto));
        fieldsPerClass.put(relatedEntity, parseFieldNames(relatedEntity));

        relationPerClass.put(dto, parseRelationFieldNames(dto));
        relationPerClass.put(relatedEntity, parseRelationFieldNames(relatedEntity));
      }
    }
  }

  private Set<Class<?>> parseGraph(Class<D> dto) {
    return parseGraph(dto, new HashSet<>());
  }

  private Set<Class<?>> parseGraph(Class<?> dto, Set<Class<?>> visited) {
    if (visited.contains(dto)) {
      return visited;
    }
    visited.add(dto);

    for (Field f : getRelations(dto)) {
      Class<?> dtoType = isCollection(f.getType()) ? getGenericType(dto, f.getName()) : f.getType();
      parseGraph(dtoType, visited);
    }
    return visited;
  }

  /**
   * <p>
   * Returns a new dto mapped with the fields of a given entity. The given
   * selected fields per class map which fields to apply from a source class to a
   * target. A set of given relation field names designates which fields are
   * mapped as relations.
   * <p>
   *
   * <p>
   * The base attributes of a source are assumed to be of the same type as the
   * target. Relations fields of a source are NOT expected to be of the same type.
   * <p>
   *
   * <p>
   * Selected fields per class should also contain the relations source class and
   * target fields to map.
   * <p>
   * 
   * @param entity                - source of the mapping
   * @param selectedFieldPerClass - selected fields of source classes to map
   * @param relations             - Set of relation field names
   * @return - A new instance of a class with the mapped fields
   */
  @SneakyThrows
  public D toDto(E entity, Map<Class<?>, Set<String>> selectedFieldPerClass, Set<String> includedRelations) {
    D dto = dtoClass.getConstructor().newInstance();

    // Include shallow references to relations not explicitly included:
    Set<String> shallowRelations = new HashSet<>(relationPerClass.get(dtoClass));
    shallowRelations.removeAll(includedRelations);
  
    mapSourceToTarget(entity, dto, selectedFieldPerClass, includedRelations, shallowRelations, new IdentityHashMap<>());
    return dto;
  }

  /**
   * <p>
   * Apply the fields of a given dto to a given entity. The given selected fields
   * per class map which fields to apply from a source class to a target. A set of
   * given relation field names designates which fields are mapped as relations.
   * <p>
   *
   * <p>
   * The base attributes of a source are assumed to be of the same type as the
   * target. Relations fields of a source are NOT expected to be of the same type.
   * <p>
   *
   * <p>
   * Selected fields per class should also contain the relations source class and
   * target fields to map.
   * <p>
   *
   * @param dto                   - source of the mapping
   * @param entity                - target of the mapping
   * @param selectedFieldPerClass - selected fields of source classes to map
   * @param relations             - Set of relation field names
   */
  @SneakyThrows
  public void applyDtoToEntity(
    D dto,
    E entity,
    Map<Class<?>, Set<String>> selectedFieldPerClass,
    Set<String> includedRelations
  ) {
    mapSourceToTarget(dto, entity, selectedFieldPerClass, includedRelations, Collections.emptySet(), new IdentityHashMap<>());
  }

  /**
   * Map the given selected fields of a source to a target with the given
   * relations.
   *
   * @param <T>
   *                                - target type
   * @param <S>
   *                                - source type
   * @param source
   *                                - source of the mapping
   * @param target
   *                                - target of the mapping
   * @param selectedFieldPerClass
   *                                - selected fields to map
   * @param includedRelations
   *                                - relations to map
   * @param visited
   *                                - map of visted objects and there corresponding target.
   */
  private <T,S> void mapSourceToTarget(
    @NonNull S source,
    @NonNull T target,
    @NonNull Map<Class<?>, Set<String>> selectedFieldPerClass,
    @NonNull Set<String> includedRelations,
    @NonNull Set<String> shallowRelations,
    @NonNull Map<Object, Object> visited
  ) {
    visited.putIfAbsent(source, target);
    Class<?> sourceType = source.getClass();
    Set<String> selectedFields = selectedFieldPerClass.getOrDefault(sourceType, new HashSet<>());
    Predicate<String> ignoreIf = field -> handlers.containsKey(sourceType)
        && handlers.get(sourceType).hasCustomFieldResolver(field);

    mapFieldsToTarget(source, target, selectedFields, ignoreIf);
    mapRelationsToTarget(source, target, selectedFieldPerClass, includedRelations, shallowRelations, visited);
    if (handlers.containsKey(sourceType)) {
      handlers.get(sourceType).resolveFields(selectedFields, source, target);
    }
  }

  /**
   * Maps the relation of a given source to a given target. The relation is
   * designated from the given field name and only the given fields per relation
   * source class are mapped.
   *
   * @param <T>
   *                         - Type of target
   * @param <S>
   *                         - Type of source
   * @param source
   *                         - source of the mapping
   * @param target
   *                         - target of the mapping
   * @param fieldsPerClass
   *                         - selected fields of the relations source class
   * @param fieldName
   *                         - field name of the relation
   * @param visited
   *                         - map of visted objects and there corresponding target.
   */
  @SneakyThrows
  private <T, S> void mapRelationsToTarget(
    S source,
    T target,
    Map<Class<?>, Set<String>> fieldsPerClass,
    Set<String> includeRelations,
    Set<String> shallowRelations,
    Map<Object, Object> visited
  ) {
    for (String relationFieldName : includeRelations) {
      if (!hasfield(source.getClass(), relationFieldName)
          || !hasfield(target.getClass(), relationFieldName)) {
        continue;
      }

      // Each relation requires a sepearte tracking set
      Map<Object, Object> currentVisited = new IdentityHashMap<>(visited);

      Class<?> sourceRelationType = PropertyUtils.getPropertyType(source, relationFieldName);
      Class<?> targetType = getResolvedType(target, relationFieldName);

      Object sourceRelation = PropertyUtils.getProperty(source, relationFieldName);
      Object targetRelation = null;

      if (sourceRelation != null) {
        if (isCollection(sourceRelationType)) {
          targetRelation = ((Collection<?>) sourceRelation).stream()
            .map(ele -> mapRelation(fieldsPerClass, ele, targetType, currentVisited))
            .collect(Collectors.toCollection(ArrayList::new));
        } else {
          targetRelation = mapRelation(fieldsPerClass, sourceRelation, targetType, currentVisited);
        }
      }

      PropertyUtils.setProperty(target, relationFieldName, targetRelation);
    }
    // For each shallow relation create the target object with only the ID field set:
    for (String relationFieldName : shallowRelations) {
      Class<?> sourceReferenceType = PropertyUtils.getPropertyType(source, relationFieldName);

      // Skip if either class is missing the field, or if the field is a collection type:
      if (!hasfield(source.getClass(), relationFieldName)
          || !hasfield(target.getClass(), relationFieldName)
          || isCollection(sourceReferenceType)) {
        continue;
      }

      Class<?> targetReferenceType = PropertyUtils.getPropertyType(target, relationFieldName);

      Object sourceReference = PropertyUtils.getProperty(source, relationFieldName);
      String idField = FieldUtils.getFieldsListWithAnnotation(targetReferenceType, JsonApiId.class)
        .stream()
        .findFirst()
        .map(Field::getName)
        .orElse(null);

      // Skip if there is no source to map from or if the target has no ID field:
      if (sourceReference == null || idField == null) {
        continue;
      }

      // Get the shallow reference's ID value (e.g. the UUID):
      Object id = PropertyUtils.getNestedProperty(source, relationFieldName + "." + idField);

      // Create the shallow reference with only the ID set:
      Object shallowReference = targetReferenceType.getDeclaredConstructor().newInstance();
      PropertyUtils.setProperty(shallowReference, idField, id);

      // Set the shallow reference into the source object's relation field:
      PropertyUtils.setProperty(target, relationFieldName, shallowReference);
    }
  }

  /**
   * Maps the given fields of a source object to new instance of a target type.
   * mapped target is returned.
   *
   * @param fields
   *                     - fields to map
   * @param source
   *                     - source of the mapping
   * @param targetType
   *                     - target type of new target
   * @param visited
   *                     - map of visted objects and there corresponding target.
   * @return the mapped target
   */
  @SneakyThrows
  private Object mapRelation(
    Map<Class<?>, Set<String>> fields,
    Object source,
    Class<?> targetType,
    Map<Object, Object> visited
  ) {
    if (source == null) {
      return null;
    }

    if (visited.keySet().contains(source)) {
      return visited.get(source);
    }

    Object target = targetType.getDeclaredConstructor().newInstance();

    Set<String> set1 = relationPerClass.getOrDefault(source.getClass(), Collections.emptySet());
    Set<String> set2 = relationPerClass.getOrDefault(targetType, Collections.emptySet());

    /**
     * Here we check which side had the relationships ( source or target ), only one
     * side contains the relationships.
     */
    if (CollectionUtils.isNotEmpty(set1)) {
      mapSourceToTarget(source, target, fields, set1, Collections.emptySet(), visited);
    } else if (CollectionUtils.isNotEmpty(set2)) {
      mapSourceToTarget(source, target, fields, set2, Collections.emptySet(), visited);
    } else {
      mapSourceToTarget(source, target, fields, Collections.emptySet(), Collections.emptySet(), visited);
    }
    return target;
  }

  /**
   * Maps the given fields of a given source to a given target.
   *
   * @param <T>            - Type of target
   * @param <S>            - Type of source
   * @param source         - source of the mapping
   * @param target         - target of the mapping
   * @param selectedFields - Selected fields to apply
   */
  @SneakyThrows
  private static <T, S> void mapFieldsToTarget(
    S source,
    T target,
    Set<String> selectedFields,
    Predicate<String> ignoreIf
  ) {
    for (String attribute : selectedFields) {
      if (!ignoreIf.test(attribute)) {
        PropertyUtils.setProperty(target, attribute, PropertyUtils.getProperty(source, attribute));
      }
    }
  }

  /**
   * Returns the class of the paramterized type at the first position of a given
   * class's given field.
   *
   * given class is assumed to be a {@link ParameterizedType}
   *
   * @param <T>
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
   * Returns the resolved type of a fieldname for a given source. If the type is a
   * collection, the first generic type is returned.
   * 
   * @param source
   *                    - source object of the field
   * @param fieldName
   *                    - field name
   * @return Field type or the first genric type if the field is a collection
   */
  @SneakyThrows
  private static Class<?> getResolvedType(Object source, String fieldName) {
    Class<?> propertyType = PropertyUtils.getPropertyType(source, fieldName);
    return isCollection(propertyType) ? getGenericType(source.getClass(), fieldName) : propertyType;
  }

  /**
   * Returns true if the given class is a collection
   * 
   * @param clazz
   *                - class to check
   * @return true if the given class is a collection
   */
  private static boolean isCollection(Class<?> clazz) {
    return Collection.class.isAssignableFrom(clazz);
  }

  /**
   * Returns the JsonApiRelations for a given class.
   * 
   * @param cls
   *              - class to parse
   * @return JsonApiRelations for a given class
   */
  private static List<Field> getRelations(Class<?> cls) {
    return FieldUtils.getFieldsListWithAnnotation(cls, JsonApiRelation.class);
  }

  /**
   * Returns the JsonApiRelation field names for a given class.
   * 
   * @param cls - class to parse
   * @return JsonApiRelations field names for a given class
   */
  private static Set<String> parseRelationFieldNames(Class<?> cls) {
    return getRelations(cls).stream().map(Field::getName).collect(Collectors.toSet());
  }

  /**
   * Returns a set of field names for a given class.
   * 
   * @param cls - class to parse
   * @return set of field names for a given class.
   */
  private static Set<String> parseFieldNames(Class<?> cls) {
    return Stream.of(cls.getDeclaredFields()).map(Field::getName).collect(Collectors.toSet());
  }

  /**
   * Returns true if the given class has the given field.
   * 
   * @param cls       - class to check
   * @param fieldName - field to check
   * @return true if the given class has the given field.
   */
  private boolean hasfield(Class<?> cls, String fieldName) {
    return fieldsPerClass.containsKey(cls) && fieldsPerClass.get(cls).contains(fieldName);
  }
}
