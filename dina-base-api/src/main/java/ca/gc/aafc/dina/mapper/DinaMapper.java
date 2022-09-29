package ca.gc.aafc.dina.mapper;

import ca.gc.aafc.dina.dto.RelatedEntity;
import ca.gc.aafc.dina.security.TextHtmlSanitizer;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.collections.CollectionUtils;
import org.hibernate.Hibernate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * DTO to Entity Bean mapper (and vice-versa). Used to map fields between DTO's and Entities.
 *
 * @param <D> Type of Dto
 * @param <E> Type of Entity
 */
@Log4j2
@AllArgsConstructor
public class DinaMapper<D, E> {

  private final Class<D> dtoClass;
  private final DinaMappingRegistry registry;

  /**
   * <p>
   * Used to construct an instance of the dina mapper where the dto class will be scanned and all
   * custom field handlers needed to resolve the entity graph will parsed from the given dto class.
   * <p>
   *
   * @param dtoClass - class to map
   */
  public DinaMapper(Class<D> dtoClass) {
    this(dtoClass, new DinaMappingRegistry(dtoClass));
  }

  /**
   * Returns a new dto mapped from the given entity. All attributes and relationships will be mapped
   * by default. If you want to select which attributes or relations to map use method signature
   * {@link DinaMapper#toDto(Object, Map, Set)}
   *
   * @param entity - source of the mapping
   * @return a new dto mapped from the given entity.
   */
  public D toDto(E entity) {
    Set<String> relations = registry.findMappableRelationsForClass(dtoClass).stream()
      .map(DinaMappingRegistry.InternalRelation::getName).collect(Collectors.toSet());
    return toDto(entity, registry.getAttributesPerClass(), relations);
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
    mapSourceToTarget(entity, dto, selectedFieldPerClass, relations, new IdentityHashMap<>(), false);
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
    mapSourceToTarget(dto, entity, selectedFieldPerClass, relations, new IdentityHashMap<>(), true);
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
   * @param visited               - map of visited objects and there corresponding target.
   * @param useSafeGet            - see {@link #mapFieldsToTarget(Object, Object, Set, boolean)}
   */
  @SuppressWarnings("unchecked")
  private <T, S> void mapSourceToTarget(
    @NonNull S source,
    @NonNull T target,
    @NonNull Map<Class<?>, Set<String>> selectedFieldPerClass,
    @NonNull Set<String> relations,
    @NonNull Map<Object, Object> visited,
    boolean useSafeGet
  ) {
    visited.putIfAbsent(source, target);
    // The source could be a Hibernate-proxied entity; unproxy it here:
    S unproxied = (S) Hibernate.unproxy(source);
    Class<?> sourceType = unproxied.getClass();
    Set<String> selectedFields = selectedFieldPerClass.getOrDefault(sourceType, Set.of());

    mapFieldsToTarget(unproxied, target, selectedFields, useSafeGet);
    mapRelationsToTarget(unproxied, target, selectedFieldPerClass, relations, visited);

    registry.findFieldAdapterForClass(sourceType)
        .ifPresent(fa -> fa.resolveFields(unproxied, target));
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
      DinaMappingRegistry.InternalRelation internalRelation = findInternalRelation(
        target, relationFieldName);

      if (internalRelation != null) {

        // Each relation requires a separate tracking set
        Map<Object, Object> currentVisited = new IdentityHashMap<>(visited);

        Class<?> targetType =  target.getClass().isAnnotationPresent(RelatedEntity.class)
          ? internalRelation.getDtoType() : internalRelation.getEntityType();
        Object sourceRelation = PropertyUtils.getProperty(source, relationFieldName);
        Object targetRelation = null;

        if (sourceRelation != null) {
          if (internalRelation.isCollection()) {
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
        log.trace("Mapping relation [{}] of class [{}]", () -> relationFieldName, targetType::toString);
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

    Object unBoxed = Hibernate.unproxy(source);
    if (visited.containsKey(unBoxed)) {
      return visited.get(unBoxed);
    }

    Object target = targetType.getDeclaredConstructor().newInstance();

    Set<String> set1 = registry.findMappableRelationsForClass(unBoxed.getClass()).stream()
      .map(DinaMappingRegistry.InternalRelation::getName).collect(Collectors.toSet());
    Set<String> set2 = registry.findMappableRelationsForClass(targetType).stream()
      .map(DinaMappingRegistry.InternalRelation::getName).collect(Collectors.toSet());

    /*
     * Here we check which side had the relationships ( source or target ), only one
     * side contains the relationships.
     */
    if (CollectionUtils.isNotEmpty(set1)) {
      mapSourceToTarget(unBoxed, target, fields, set1, visited, false);
    } else if (CollectionUtils.isNotEmpty(set2)) {
      mapSourceToTarget(unBoxed, target, fields, set2, visited, false);
    } else {
      mapSourceToTarget(unBoxed, target, fields, Collections.emptySet(), visited, false);
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
   * @param useSafeGet     - Should a safeGet be used to map the field from source to destination. Should be used
   *                       when the data is coming from an untrusted source (from DTO received on-the-wire)
   */
  @SneakyThrows
  private static <T, S> void mapFieldsToTarget(
    S source,
    T target,
    Set<String> selectedFields, boolean useSafeGet
  ) {
    for (String attribute : selectedFields) {
      log.trace("Mapping property [{}] of class [{}]", () -> attribute, () -> source.getClass().toString());
      PropertyUtils.setProperty(target, attribute,
              useSafeGet ? safeGetProperty(PropertyUtils.getProperty(source, attribute), attribute) :
                      PropertyUtils.getProperty(source, attribute));
    }
  }

  /**
   * Ensures a value is safe before moving it from a source to target.
   * Limited to String field for now.
   * @param property
   * @param attribute
   * @return
   */
  private static Object safeGetProperty(Object property, String attribute) {
    if (property instanceof String text) {
      if (TextHtmlSanitizer.isSafeText(text)) {
        return property;
      } else {
        throw new IllegalArgumentException("unsafe value detected in attribute " + attribute);
      }
    }
    return property;
  }

  private <T> DinaMappingRegistry.InternalRelation findInternalRelation(
    T target,
    String relationFieldName
  ) {
    return registry.findMappableRelationsForClass(target.getClass())
      .stream()
      .filter(ir -> ir.getName().equalsIgnoreCase(relationFieldName))
      .findFirst()
      .orElse(null);
  }
}
