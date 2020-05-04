package ca.gc.aafc.dina.mapper;

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
      Class<?> targetRelationType = PropertyUtils.getPropertyType(target, relationFieldName);

      Object targetRelationObject = targetRelationType.getConstructor().newInstance();
      Object sourceRelationObject = PropertyUtils.getProperty(source, relationFieldName);

      Set<String> selectedRelationFields = selectedFieldPerClass.getOrDefault(sourceRelationType, new HashSet<>());

      mapFieldsToTarget(sourceRelationObject, targetRelationObject, selectedRelationFields);
      PropertyUtils.setProperty(target, relationFieldName, targetRelationObject);
    }
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
    return !dtoResolvers.stream()
        .filter(cfr->StringUtils.equalsIgnoreCase(fieldName, cfr.getField()))
        .collect(Collectors.toList()).isEmpty();
  }

}
