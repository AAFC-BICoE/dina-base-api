package ca.gc.aafc.dina.mapper;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.StringUtils;

import lombok.NonNull;
import lombok.SneakyThrows;

public class DinaMapper<D, E> {

  private final Class<D> dtoClass;
  private final Class<E> entityClass;

  private final List<CustomFieldResolverSpec<E>> dtoResolvers;
  private final List<CustomFieldResolverSpec<D>> entityResolvers;

  public DinaMapper(
    @NonNull Class<D> dtoClass,
    @NonNull Class<E> entityClass,
    @NonNull List<CustomFieldResolverSpec<E>> dtoResolvers,
    @NonNull List<CustomFieldResolverSpec<D>> entityResolvers
  ) {
    this.dtoClass = dtoClass;
    this.entityClass = entityClass;
    this.dtoResolvers = dtoResolvers;
    this.entityResolvers = entityResolvers;
  }

  @SneakyThrows
  public D toDto(
    @NonNull E entity,
    @NonNull Map<Class<?>, Set<String>> selectedFieldPerClass,
    @NonNull Set<String> relations
  ) {
    D dto = dtoClass.getConstructor().newInstance();
    Set<String> selectedFields = selectedFieldPerClass.getOrDefault(entityClass, new HashSet<>());

    // Map non relations and non custom resolved fields
    Set<String> selectedBaseFields = selectedFields
      .stream()
      .filter(sf -> !hasCustomFieldResolver(sf))
      .collect(Collectors.toSet());
    mapFieldsToTarget(entity, dto, selectedBaseFields);

    // Map Relations
    mapRelationsToTarget(entity, dto, selectedFieldPerClass, relations);

    // Map selected Custom Fields
    List<CustomFieldResolverSpec<E>> selectedResolvers = dtoResolvers
      .stream()
      .filter(cfr-> selectedFields.contains(cfr.getField()))
      .collect(Collectors.toList());
    mapCustomFieldsToTarget(entity, dto, selectedResolvers);

    return dto;
  }

  @SneakyThrows
  public void applyDtoToEntity(
    @NonNull D dto,
    @NonNull E entity,
    @NonNull Map<Class<?>, Set<String>> selectedFieldPerClass,
    @NonNull Set<String> relations
  ) {
    Set<String> selectedFields = selectedFieldPerClass.getOrDefault(dtoClass, new HashSet<>());

    // Map non relations and non custom resolved fields
    Set<String> selectedBaseFields = selectedFields
      .stream()
      .filter(sf -> !hasCustomFieldResolver(sf))
      .collect(Collectors.toSet());
    mapFieldsToTarget(dto, entity, selectedBaseFields);

    // Map Relations
    mapRelationsToTarget(dto, entity, selectedFieldPerClass, relations);

    // Map selected Custom Fields
    List<CustomFieldResolverSpec<D>> selectedResolvers = entityResolvers
      .stream()
      .filter(cfr-> selectedFields.contains(cfr.getField()))
      .collect(Collectors.toList());
    mapCustomFieldsToTarget(dto, entity, selectedResolvers);
  }

  @SneakyThrows
  private static <T, S> void mapRelationsToTarget(
    S source,
    T target,
    Map<Class<?>, Set<String>> selectedFieldPerClass,
    Set<String> relations
  ) {
    for (String relationFieldName : relations) {
      Class<?> sourceRelationType = PropertyUtils.getPropertyType(source, relationFieldName);

      if (Collection.class.isAssignableFrom(sourceRelationType)) {
        mapCollectionRelation(source, target, selectedFieldPerClass, relationFieldName);
      } else {
        mapSingleRelation(source, target, selectedFieldPerClass, relationFieldName);
      }
    }
  }

  @SneakyThrows
  private static <T, S> void mapCollectionRelation(
    S source,
    T target,
    Map<Class<?>, Set<String>> selectedFieldPerClass,
    String fieldName
  ) {
    Collection<Object> sourceCollection = (Collection<Object>) PropertyUtils.getProperty(source, fieldName);

    if (sourceCollection != null) {
      Collection<Object> targetCollection = null;

      if (sourceCollection instanceof List<?>) {
        targetCollection = new ArrayList<>();
      }

      Class<?> targetElementType = getGenericType(target.getClass(), fieldName);

      for (Object relationElement : sourceCollection) {
        Object targetElement = targetElementType.newInstance();
        mapFieldsToTarget(
          relationElement,
          targetElement,
          selectedFieldPerClass.getOrDefault(relationElement.getClass(), new HashSet<>())
        );
        targetCollection.add(targetElement);
      }
      PropertyUtils.setProperty(target, fieldName, targetCollection);
    } else {
      PropertyUtils.setProperty(target, fieldName, null);
    }

  }

  @SneakyThrows
  private static <T, S> void mapSingleRelation(
    S source,
    T target,
    Map<Class<?>, Set<String>> selectedFieldPerClass,
    String fieldName
  ) {
    Class<?> sourceRelationType = PropertyUtils.getPropertyType(source, fieldName);
    Class<?> targetRelationType = PropertyUtils.getPropertyType(target, fieldName);

    Object targetRelationObject = targetRelationType.getConstructor().newInstance();
    Object sourceRelationObject = PropertyUtils.getProperty(source, fieldName);

    Set<String> selectedRelationFields = selectedFieldPerClass.getOrDefault(sourceRelationType, new HashSet<>());

    mapFieldsToTarget(sourceRelationObject, targetRelationObject, selectedRelationFields);
    PropertyUtils.setProperty(target, fieldName, targetRelationObject);
  }

  @SneakyThrows
  private static <T, S> void mapFieldsToTarget(S source, T target, Set<String> selectedFields) {
    for (String attribute : selectedFields) {
      PropertyUtils.setProperty(target, attribute, PropertyUtils.getProperty(source, attribute));
    }
  }

  @SneakyThrows
  private static <T, S> void mapCustomFieldsToTarget(S source, T target, List<CustomFieldResolverSpec<S>> resolvers) {
    for (CustomFieldResolverSpec<S> cfr : resolvers) {
      String fieldName = cfr.getField();
      PropertyUtils.setProperty(target, fieldName, cfr.getResolver().apply(source));
    }
  }

  /**
   * Returns true if the given field name has a custom resolver
   * 
   * @param fieldName - field name of field to check
   * @return - true if the given field has custom resolvers.
   */
  private boolean hasCustomFieldResolver(String fieldName) {
    boolean hasDtoResolvers = !dtoResolvers.stream()
        .filter(cfr->StringUtils.equalsIgnoreCase(fieldName, cfr.getField()))
        .collect(Collectors.toList())
        .isEmpty();
    boolean hasEntityResolvers = !entityResolvers.stream()
        .filter(cfr -> StringUtils.equalsIgnoreCase(fieldName, cfr.getField()))
        .collect(Collectors.toList())
        .isEmpty();
    return hasDtoResolvers || hasEntityResolvers;
  }

  @SneakyThrows
  private static <T> Class<?> getGenericType(Class<?> source, String fieldName) {
    ParameterizedType genericType = (ParameterizedType) source
        .getDeclaredField(fieldName)
        .getGenericType();
    return (Class<?>) genericType.getActualTypeArguments()[0];
  }

}
