package ca.gc.aafc.dina.mapper;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
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

  public DinaMapper(@NonNull Class<D> dtoClass) {
    this(dtoClass, new HashMap<>());
    getHandlers(dtoClass, handlers);
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
  public D toDto(E entity, Map<Class<?>, Set<String>> selectedFieldPerClass, Set<String> relations) {
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
    D dto,
    E entity,
    Map<Class<?>, Set<String>> selectedFieldPerClass,
    Set<String> relations
  ) {
    mapSourceToTarget(dto, entity, selectedFieldPerClass, relations);
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
   * @param relations
   *                                - relations to map
   */
  private <T,S> void mapSourceToTarget(
    @NonNull S source,
    @NonNull T target,
    @NonNull Map<Class<?>, Set<String>> selectedFieldPerClass,
    @NonNull Set<String> relations
  ) {
    Class<?> sourceType = source.getClass();
    Set<String> selectedFields = selectedFieldPerClass.getOrDefault(sourceType, new HashSet<>());
    Predicate<String> ignoreIf = field -> handlers.containsKey(sourceType)
        && handlers.get(sourceType).hasCustomFieldResolver(field);

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
      Class<?> targetType = getResolvedType(target, relationFieldName);

      Object sourceRelation = PropertyUtils.getProperty(source, relationFieldName);
      Object targetRelation = null;

      if (sourceRelation != null) {
        if (isCollection(sourceRelationType)) {
          targetRelation = ((Collection<?>) sourceRelation).stream()
            .map(ele -> mapRelation(fieldsPerClass, ele, targetType))
            .collect(Collectors.toCollection(ArrayList::new));
        } else {
          targetRelation = mapRelation(fieldsPerClass, sourceRelation, targetType);
        }
      }

      PropertyUtils.setProperty(target, relationFieldName, targetRelation);
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
   * @return the mapped target
   */
  @SneakyThrows
  private Object mapRelation(Map<Class<?>, Set<String>> fields, Object source, Class<?> targetType) {
    if (source == null) {
      return null;
    }

    Object target = targetType.newInstance();
    Set<String> relation = Stream
      .concat(getRelations(source.getClass()).stream(), getRelations(targetType).stream())
      .map(Field::getName).collect(Collectors.toSet());
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
  private static Class<?> getGenericType(Class<?> source, String fieldName) {
    ParameterizedType genericType = (ParameterizedType) source
        .getDeclaredField(fieldName)
        .getGenericType();
    return (Class<?>) genericType.getActualTypeArguments()[0];
  }

  /**
   * Fills a given map with all Custom Field Handlers needed to map a given class
   * parsed from the given class, This includes Custom Field Handlers for each
   * relationship of a given class.
   * 
   * @param <T>
   *                - class type
   * @param clazz
   *                - class to parse
   * @param map
   *                - map to fill
   */
  private static <T> void getHandlers(Class<T> clazz, Map<Class<?>, CustomFieldHandler<?, ?>> map) {
    Class<?> relatedEntity = clazz.getAnnotation(RelatedEntity.class).value();

    if (map.containsKey(clazz) || map.containsKey(relatedEntity)) {
      return;
    }

    CustomFieldHandler<?, ?> handler = new CustomFieldHandler<>(clazz, relatedEntity);
    map.put(clazz, handler);
    map.put(relatedEntity, handler);

    for (Field field : getRelations(clazz)) {
      Class<?> dtoType = isCollection(field.getType()) 
        ? getGenericType(clazz, field.getName()) 
        : field.getType();
      getHandlers(dtoType, map);
    }
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
}
