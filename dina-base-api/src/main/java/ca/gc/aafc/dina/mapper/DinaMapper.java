package ca.gc.aafc.dina.mapper;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.beanutils.PropertyUtils;

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

  private final CustomFieldHandler<D, E> resolverHandler;

  public DinaMapper(@NonNull Class<D> dtoClass, @NonNull Class<E> entityClass) {
    this.dtoClass = dtoClass;
    this.entityClass = entityClass;
    this.resolverHandler = new CustomFieldHandler<>(dtoClass, entityClass);
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

    mapSourceToTarget(
      entity,
      dto,
      selectedFieldPerClass,
      relations,
      resolverHandler::hasCustomFieldResolver);

    // Map selected Custom Fields
    Set<String> selectedFields = selectedFieldPerClass.getOrDefault(entityClass, new HashSet<>());
    resolverHandler.resolveDtoFields(selectedFields, entity, dto);
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
    mapSourceToTarget(
      dto,
      entity,
      selectedFieldPerClass,
      relations,
      resolverHandler::hasCustomFieldResolver);

    // Map selected Custom Fields
    Set<String> selectedFields = selectedFieldPerClass.getOrDefault(dtoClass, new HashSet<>());
    resolverHandler.resolveEntityFields(selectedFields, dto, entity);
  }

  private static <T, S> void mapSourceToTarget(
    S source,
    T target,
    Map<Class<?>, Set<String>> selectedFieldPerClass,
    Set<String> relations,
    Predicate<String> ignoreIf
  ) {
    Set<String> selectedFields = selectedFieldPerClass.getOrDefault(
      source.getClass(),
      new HashSet<>());

    // Map non relations and non custom resolved fields
    Set<String> selectedBaseFields = selectedFields.stream()
      .filter(sf -> ignoreIf.negate().test(sf))
      .collect(Collectors.toSet());
    mapFieldsToTarget(source, target, selectedBaseFields);

    // Map Relations
    mapRelationsToTarget(source, target, selectedFieldPerClass, relations, ignoreIf);
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
   * @param selectedFieldPerClass - selected fields of the relations source class
   * @param fieldName             - field name of the relation
   */
  @SneakyThrows
  private static <T, S> void mapRelationsToTarget(
    S source,
    T target,
    Map<Class<?>, Set<String>> selectedFieldPerClass,
    Set<String> relations,
    Predicate<String> ignoreIf
  ) {
    for (String relationFieldName : relations) {
      Class<?> sourceRelationType = PropertyUtils.getPropertyType(source, relationFieldName);

      if (Collection.class.isAssignableFrom(sourceRelationType)) {
        mapCollectionRelation(source, target, selectedFieldPerClass, relationFieldName, ignoreIf);
      } else {
        mapSingleRelation(source, target, selectedFieldPerClass, relationFieldName, ignoreIf);
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
   * @param fieldName             - field name of the relation
   */
  @SneakyThrows
  private static <T, S> void mapCollectionRelation(
    S source,
    T target,
    Map<Class<?>, Set<String>> selectedFieldPerClass,
    String fieldName,
    Predicate<String> ignoreIf
  ) {
    Collection<?> sourceCollection = (Collection<?>) PropertyUtils.getProperty(source, fieldName);
    Collection<Object> targetCollection = null;

    if (sourceCollection != null) {

      if (sourceCollection instanceof List<?>) {
        targetCollection = new ArrayList<>();
      }

      Class<?> targetElementType = getGenericType(target.getClass(), fieldName);

      for (Object sourceElement : sourceCollection) {
        Object targetElement = targetElementType.newInstance();
        mapSourceToTarget(
          sourceElement,
          targetElement,
          selectedFieldPerClass,
          new HashSet<>(),
          ignoreIf);
        targetCollection.add(targetElement);
      }
    }
    PropertyUtils.setProperty(target, fieldName, targetCollection);
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
  private static <T, S> void mapSingleRelation(
    S source,
    T target,
    Map<Class<?>, Set<String>> selectedFieldPerClass,
    String fieldName,
    Predicate<String> ignoreIf
  ) {
    Object sourceRelationObject = PropertyUtils.getProperty(source, fieldName);
    Object targetRelationObject = null;

    if (sourceRelationObject != null) {
      targetRelationObject = PropertyUtils.getPropertyType(target, fieldName)
        .getConstructor()
        .newInstance();

      mapSourceToTarget(
        sourceRelationObject,
        targetRelationObject,
        selectedFieldPerClass,
        new HashSet<>(),
        ignoreIf);
    }
    PropertyUtils.setProperty(target, fieldName, targetRelationObject);
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
  private static <T, S> void mapFieldsToTarget(S source, T target, Set<String> selectedFields) {
    for (String attribute : selectedFields) {
      PropertyUtils.setProperty(target, attribute, PropertyUtils.getProperty(source, attribute));
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

}
