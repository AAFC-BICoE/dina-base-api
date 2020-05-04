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

    // Map non relations and non custom resolved fields
    Set<String> selectedBaseFields = selectedFieldPerClass
      .getOrDefault(entityClass, new HashSet<>())
      .stream()
      .filter(sf -> !hasCustomFieldResolver(sf))
      .collect(Collectors.toSet());
    mapFieldsToTarget(entity, dto, selectedBaseFields);

    // Map Relations
    mapRelationsToTarget(entity, dto, selectedFieldPerClass, relations);

    // Map Custom Fields
    for (CustomFieldResolverSpec<E> cfr : dtoResolvers) {
      String fieldName = cfr.getField();
      if (selectedFieldPerClass.getOrDefault(entityClass, new HashSet<>()).contains(fieldName)) {
        PropertyUtils.setProperty(dto, fieldName, cfr.getResolver().apply(entity));
      }
    }

    return dto;
  }

  @SneakyThrows
  public void applyDtoToEntity(
    @NonNull D dto,
    @NonNull E entity,
    @NonNull Map<Class<?>, Set<String>> selectedFieldPerClass,
    @NonNull Set<String> relations
  ) {

    // Map non relations and non custom resolved fields
    Set<String> selectedBaseFields = selectedFieldPerClass.get(dtoClass)
      .stream()
      .filter(sf -> !hasCustomFieldResolver(sf))
      .collect(Collectors.toSet());
    mapFieldsToTarget(dto, entity, selectedBaseFields);

    // Map Relations
    mapRelationsToTarget(dto, entity, selectedFieldPerClass, relations);

    // Map Custom Fields
    for (CustomFieldResolverSpec<D> cfr : entityResolvers) {
      String fieldName = cfr.getField();
      if (selectedFieldPerClass.get(dtoClass).contains(fieldName)) {
        PropertyUtils.setProperty(entity, fieldName, cfr.getResolver().apply(dto));
      }
    }
  }

  @SneakyThrows
  private static <T, S> void mapRelationsToTarget(
    S source,
    T target,
    Map<Class<?>, Set<String>> selectedFieldPerClass,
    Set<String> relations
  ) {
    for (String relationFieldName : relations) {
      Class<?> relationType = PropertyUtils.getPropertyType(target, relationFieldName);

      Object targetRelationObject = relationType.getConstructor().newInstance();
      Object sourceRelationObject = PropertyUtils.getProperty(source, relationFieldName);

      Set<String> selectedRelationFields = selectedFieldPerClass.getOrDefault(relationType, new HashSet<>());

      mapFieldsToTarget(sourceRelationObject, targetRelationObject, selectedRelationFields);
      PropertyUtils.setProperty(target, relationFieldName, targetRelationObject);
    }
  }

  @SneakyThrows
  private static <T, S> void mapFieldsToTarget(S source, T target, Set<String> selectedFieldPerClass) {
    for (String attribute : selectedFieldPerClass) {
      PropertyUtils.setProperty(target, attribute, PropertyUtils.getProperty(source, attribute));
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
