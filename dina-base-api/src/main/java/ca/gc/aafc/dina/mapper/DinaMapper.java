package ca.gc.aafc.dina.mapper;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

import ca.gc.aafc.dina.dto.RelatedEntity;
import io.crnk.core.resource.annotations.JsonApiRelation;
import lombok.NonNull;
import lombok.SneakyThrows;

/**
 * DTO to Entity Bean mapper (and vice-versa). Used to map fields between DTO's
 * and Entities.
 *
 * @param <D> - Type of Dto
 * @param <E> - Type of Entity
 */
public class DinaMapper<D, E> {

  private final Class<D> dtoClass;
  private final Class<E> entityClass;
  private final Map<Class<?>, CustomFieldHandler<?, ?>> handlers = new HashMap<>();

  public DinaMapper(@NonNull Class<D> dtoClass, @NonNull Class<E> entityClass) {
    this.dtoClass = dtoClass;
    this.entityClass = entityClass;
    getHandlers(dtoClass, handlers);
  }

  private static <T> void getHandlers(Class<T> clazz, Map<Class<?>, CustomFieldHandler<?, ?>> map) {
    Class<?> relatedEntity = clazz.getAnnotation(RelatedEntity.class).value();

    if (map.containsKey(clazz) || map.containsKey(relatedEntity)) {
      return;
    }

    CustomFieldHandler<?, ?> handler = new CustomFieldHandler<>(clazz, relatedEntity);
    map.put(clazz, handler);
    map.put(relatedEntity, handler);

    getRelationsForClasses(clazz).forEach(rel -> {
      Class<?> dtoType = isCollection(rel.getType()) 
        ? getGenericType(clazz, rel.getName()) 
        : rel.getType();
      getHandlers(dtoType, map);
    });
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
  public D toDto(
    @NonNull E entity,
    @NonNull Map<Class<?>, Set<String>> selectedFieldPerClass,
    @NonNull Set<String> relations
  ) {
    D dto = dtoClass.getConstructor().newInstance();
    mapSourceToTarget(entity, dto, selectedFieldPerClass, relations);
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
    @NonNull D dto,
    @NonNull E entity,
    @NonNull Map<Class<?>, Set<String>> selectedFieldPerClass,
    @NonNull Set<String> relations
  ) {
    mapSourceToTarget(dto, entity, selectedFieldPerClass, relations);
  }

  private <T,S> void mapSourceToTarget(
    S source,
    T target,
    Map<Class<?>, Set<String>> selectedFieldPerClass,
    Set<String> relations
  ) {
    Class<?> sourceType = source.getClass();
    Set<String> selectedFields = selectedFieldPerClass.getOrDefault(sourceType, new HashSet<>());
    Predicate<String> ignoreIf = field -> hasCustomFieldResolver(sourceType, field);

    mapFieldsToTarget(source, target, selectedFields, ignoreIf);
    mapRelationsToTarget(source, target, selectedFieldPerClass, relations);
    handlers.get(sourceType).resolveFields(selectedFields, source, target);
  }

  /**
   * Maps the relation of a given source to a given target. The relation is
   * designated from the given field name and only the given fields per relation
   * source class are mapped.
   *
   * @param <T>                   - Type of target
   * @param <S>                   - Type of source
   * @param source                - source of the mapping
   * @param target                - target of the mapping
   * @param fieldsPerClass - selected fields of the relations source class
   * @param fieldName             - field name of the relation
   */
  @SneakyThrows
  private <T, S> void mapRelationsToTarget(
    S source,
    T target,
    Map<Class<?>, Set<String>> fieldsPerClass,
    Set<String> relations
  ) {
    for (String relationFieldName : relations) {
      Class<?> sourceRelationType = PropertyUtils.getPropertyType(source, relationFieldName);
      if (isCollection(sourceRelationType)) {
        mapCollectionRelation(source, target, fieldsPerClass, relationFieldName);
      } else {
        mapSingleRelation(source, target, fieldsPerClass, relationFieldName);
      }
    }
  }

  /**
   * Maps the relation of a given source to a given target. The relation is
   * designated from the given field name and only the given fields per relation
   * source class are mapped. Relations are assumed to be {@link Collection}
   * objects.
   *
   * @param <T>                   - Type of target
   * @param <S>                   - Type of source
   * @param source                - source of the mapping
   * @param target                - target of the mapping
   * @param selectedFieldPerClass - selected fields of the relations source class
   * @param relation             - field name of the relation
   */
  @SneakyThrows
  private <T, S> void mapCollectionRelation(
    S source,
    T target,
    Map<Class<?>, Set<String>> fieldsPerClass,
    String relation
  ) {
    Collection<?> sourceCollection = (Collection<?>) PropertyUtils.getProperty(source, relation);
    Collection<Object> targetCollection = null;

    if (sourceCollection != null) {

      if (sourceCollection instanceof List<?>) {
        targetCollection = new ArrayList<>();
      }

      Class<?> targetElementType = getGenericType(target.getClass(), relation);

      for (Object sourceElement : sourceCollection) {
        Object targetElement = mapRelation(fieldsPerClass, sourceElement, targetElementType);
        targetCollection.add(targetElement);
      }
    }
    PropertyUtils.setProperty(target, relation, targetCollection);
  }

  /**
   * Maps the relation of a given source to a given target. The relation is
   * designated from the given field name and only the given fields per relation
   * source class are mapped. Single relations are objects which are not
   * collections or arrays.
   *
   * @param <T>                   - Type of target
   * @param <S>                   - Type of source
   * @param source                - source of the mapping
   * @param target                - target of the mapping
   * @param selectedFieldPerClass - selected fields of the relations source class
   * @param fieldName             - field name of the relation
   */
  @SneakyThrows
  private <T, S> void mapSingleRelation(
    S source,
    T target,
    Map<Class<?>, Set<String>> fieldsPerClass,
    String fieldName
  ) {
    Object sourceRelation = PropertyUtils.getProperty(source, fieldName);
    Class<?> targetType = PropertyUtils.getPropertyType(target, fieldName);
    Object targetRelation = mapRelation(fieldsPerClass, sourceRelation, targetType);
    PropertyUtils.setProperty(target, fieldName, targetRelation);
  }

  @SneakyThrows
  private Object mapRelation(Map<Class<?>, Set<String>> fields, Object source, Class<?> targetType) {
    if (source == null) {
      return null;
    }

    Object target = targetType.newInstance();
    Set<String> relation = getRelationsForClasses(source.getClass(), targetType)
      .map(Field::getName)
      .collect(Collectors.toSet());
    mapSourceToTarget(source, target, fields, relation);
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
      if (ignoreIf.negate().test(attribute)) {
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
  private static <T> Class<?> getGenericType(Class<?> source, String fieldName) {
    ParameterizedType genericType = (ParameterizedType) source
        .getDeclaredField(fieldName)
        .getGenericType();
    return (Class<?>) genericType.getActualTypeArguments()[0];
  }

  private boolean hasCustomFieldResolver(Class<?> clazz, String field) {
    return handlers.containsKey(clazz) && handlers.get(clazz).hasCustomFieldResolver(field);
  }

  private static boolean isCollection(Class<?> clazz) {
    return Collection.class.isAssignableFrom(clazz);
  }

  private static Stream<Field> getRelationsForClasses(Class<?>... classes) {
    return Arrays.stream(classes)
      .flatMap(cls -> FieldUtils.getFieldsListWithAnnotation(cls, JsonApiRelation.class).stream());
  }
}
