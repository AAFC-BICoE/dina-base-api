package ca.gc.aafc.dina.mapper;

import ca.gc.aafc.dina.dto.RelatedEntity;
import io.crnk.core.resource.annotations.JsonApiRelation;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.hibernate.Hibernate;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * DTO to Entity Bean mapper (and vice-versa). Used to map fields between DTO's and Entities.
 *
 * @param <D> - Type of Dto
 * @param <E> - Type of Entity
 */
@AllArgsConstructor
public class DinaMapper<D, E> {

  private final Class<D> dtoClass;
  private final Map<Class<?>, CustomFieldHandler<?, ?>> handlers;
  private final DinaMappingRegistry registry;

  /**
   * <p>
   * Used to construct an instance of the dina mapper where the dto class will be scanned and all
   * custom field handlers needed to resolve the entity graph will parsed from the given dto class.
   * <p>
   *
   * <p>
   * Use this constructor if you have no custom fields to resolve or you are unsure if you can
   * supply the custom field handlers per class.
   * <p>
   *
   * @param dtoClass - class to map
   */
  public DinaMapper(Class<D> dtoClass) {
    this(dtoClass, new HashMap<>(), new DinaMappingRegistry(dtoClass));
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
      }
    }
  }

  private Set<Class<?>> parseGraph(Class<D> dto) {
    return parseGraph(dto, new HashSet<>());
  }

  @SneakyThrows
  private Set<Class<?>> parseGraph(Class<?> dto, Set<Class<?>> visited) {
    if (visited.contains(dto)) {
      return visited;
    }
    visited.add(dto);

    for (Field f : FieldUtils.getFieldsListWithAnnotation(dto, JsonApiRelation.class)) {
      Class<?> dtoType = DinaMappingRegistry.getResolvedType(
        dto.getConstructor().newInstance(), f.getName());
      parseGraph(dtoType, visited);
    }
    return visited;
  }

  /**
   * <p>
   * Returns a new dto mapped with the fields of a given entity. The given selected fields per class
   * map which fields to apply from a source class to a target. A set of given relation field names
   * designates which fields are mapped as relations.
   * <p>
   *
   * <p>
   * The base attributes of a source are assumed to be of the same type as the target. Relations
   * fields of a source are NOT expected to be of the same type.
   * <p>
   *
   * <p>
   * Selected fields per class should also contain the relations source class and target fields to
   * map.
   * <p>
   *
   * @param entity                - source of the mapping
   * @param selectedFieldPerClass - selected fields of source classes to map
   * @param relations             - Set of relation field names
   * @return - A new instance of a class with the mapped fields
   */
  @SneakyThrows
  public D toDto(
    E entity,
    Map<Class<?>, Set<String>> selectedFieldPerClass,
    Set<String> relations
  ) {
    D dto = dtoClass.getConstructor().newInstance();
    mapSourceToTarget(entity, dto, selectedFieldPerClass, relations, new IdentityHashMap<>());
    return dto;
  }

  /**
   * <p>
   * Apply the fields of a given dto to a given entity. The given selected fields per class map
   * which fields to apply from a source class to a target. A set of given relation field names
   * designates which fields are mapped as relations.
   * <p>
   *
   * <p>
   * The base attributes of a source are assumed to be of the same type as the target. Relations
   * fields of a source are NOT expected to be of the same type.
   * <p>
   *
   * <p>
   * Selected fields per class should also contain the relations source class and target fields to
   * map.
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
    mapSourceToTarget(dto, entity, selectedFieldPerClass, relations, new IdentityHashMap<>());
  }

  /**
   * Map the given selected fields of a source to a target with the given relations.
   *
   * @param <T>                   - target type
   * @param <S>                   - source type
   * @param source                - source of the mapping
   * @param target                - target of the mapping
   * @param selectedFieldPerClass - selected fields to map
   * @param relations             - relations to map
   * @param visited               - map of visted objects and there corresponding target.
   */
  @SuppressWarnings("unchecked")
  private <T, S> void mapSourceToTarget(
    @NonNull S source,
    @NonNull T target,
    @NonNull Map<Class<?>, Set<String>> selectedFieldPerClass,
    @NonNull Set<String> relations,
    @NonNull Map<Object, Object> visited
  ) {
    visited.putIfAbsent(source, target);
    // The source could be a Hibernate-proxied entity; unproxy it here:
    S unproxied = (S) Hibernate.unproxy(source);
    Class<?> sourceType = unproxied.getClass();
    Set<String> selectedFields = selectedFieldPerClass.getOrDefault(sourceType, new HashSet<>());
    Predicate<String> ignoreIf = field -> hasResolvers(field, sourceType);

    mapFieldsToTarget(unproxied, target, selectedFields, ignoreIf);
    mapRelationsToTarget(unproxied, target, selectedFieldPerClass, relations, visited);
    if (handlers.containsKey(sourceType)) {
      Set<String> allFields = Stream.concat(selectedFields.stream(), relations.stream())
        .collect(Collectors.toSet());
      handlers.get(sourceType).resolveFields(allFields, unproxied, target);
    }
  }

  /**
   * Maps the relation of a given source to a given target. The relation is designated from the
   * given field name and only the given fields per relation source class are mapped.
   *
   * @param <T>            - Type of target
   * @param <S>            - Type of source
   * @param source         - source of the mapping
   * @param target         - target of the mapping
   * @param fieldsPerClass - selected fields of the relations source class
   * @param visited        - map of visted objects and there corresponding target.
   */
  @SneakyThrows
  private <T, S> void mapRelationsToTarget(
    S source,
    T target,
    Map<Class<?>, Set<String>> fieldsPerClass,
    Set<String> relations,
    Map<Object, Object> visited
  ) {
    for (String relationFieldName : relations) {
      if (!hasResolvers(relationFieldName, source.getClass())
          && containsRelation(relationFieldName, source.getClass(), target.getClass())) {

        // Each relation requires a separate tracking set
        Map<Object, Object> currentVisited = new IdentityHashMap<>(visited);

        Class<?> targetType = DinaMappingRegistry.getResolvedType(target, relationFieldName);
        Object sourceRelation = PropertyUtils.getProperty(source, relationFieldName);
        Object targetRelation = null;

        if (sourceRelation != null) {
          if (registry.isRelationCollection(source.getClass(), relationFieldName)) {
            targetRelation = ((Collection<?>) sourceRelation).stream()
              .map(ele -> mapRelation(fieldsPerClass, ele, targetType, currentVisited))
              .collect(Collectors.toCollection(ArrayList::new));
          } else {
            targetRelation = mapRelation(
              fieldsPerClass,
              sourceRelation,
              targetType,
              currentVisited);
          }
        }

        PropertyUtils.setProperty(target, relationFieldName, targetRelation);
      }
    }
  }

  /**
   * Maps the given fields of a source object to new instance of a target type. mapped target is
   * returned.
   *
   * @param fields     - fields to map
   * @param source     - source of the mapping
   * @param targetType - target type of new target
   * @param visited    - map of visted objects and there corresponding target.
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

    if (visited.containsKey(source)) {
      return visited.get(source);
    }

    Object target = targetType.getDeclaredConstructor().newInstance();

    Set<String> set1 = registry.findMappableRelationsForClass(source.getClass());
    Set<String> set2 = registry.findMappableRelationsForClass(targetType);

    /*
     * Here we check which side had the relationships ( source or target ), only one
     * side contains the relationships.
     */
    if (CollectionUtils.isNotEmpty(set1)) {
      mapSourceToTarget(source, target, fields, set1, visited);
    } else if (CollectionUtils.isNotEmpty(set2)) {
      mapSourceToTarget(source, target, fields, set2, visited);
    } else {
      mapSourceToTarget(source, target, fields, Collections.emptySet(), visited);
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
   * Returns true if the given classes all contain the given relation.
   *
   * @param field   field to check
   * @param classes classes to check
   * @return true if the given classes all contain the given field.
   */
  private boolean containsRelation(String field, Class<?>... classes) {
    return Stream.of(classes).allMatch(aClass ->
      registry.findMappableRelationsForClass(aClass) != null &&
      registry.findMappableRelationsForClass(aClass).stream().anyMatch(field::equalsIgnoreCase)
    );
  }

  /**
   * Returns true if the given class and field have custom field resolvers tracked by the mapper
   *
   * @param field  field to check
   * @param aClass class to check
   * @return rue if the given class and field have custom field resolvers tracked by the mapper.
   */
  private boolean hasResolvers(String field, Class<?> aClass) {
    return handlers.containsKey(aClass) && handlers.get(aClass).hasCustomFieldResolver(field);
  }
}
