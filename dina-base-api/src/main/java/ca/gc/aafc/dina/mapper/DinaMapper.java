package ca.gc.aafc.dina.mapper;

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
    Set<String> selectedBaseFields = selectedFieldPerClass.get(entityClass)
      .stream()
      .filter(sf -> !hasCustomFieldResolver(sf))
      .collect(Collectors.toSet());
    mapFieldsToTarget(entity, dto, selectedBaseFields);

    // Map relations
    for (String relationFieldName : relations) {
      Class<?> relationDtoType = PropertyUtils.getPropertyType(dto, relationFieldName);

      Object relationDto = relationDtoType.getConstructor().newInstance();
      Object entityRelationField = PropertyUtils.getProperty(entity, relationFieldName);

      mapFieldsToTarget(entityRelationField, relationDto, selectedFieldPerClass.get(relationDtoType));
      PropertyUtils.setProperty(dto, relationFieldName, relationDto);
    }

    // Map Custom Fields
    for (CustomFieldResolverSpec<E> cfr : dtoResolvers) {
      String fieldName = cfr.getField();
      if (selectedFieldPerClass.get(entityClass).contains(fieldName)) {
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
    mapFieldsToTarget(entity, dto, selectedBaseFields);

    // Map relations
    for (String relationFieldName : relations) {
      Class<?> relationType = PropertyUtils.getPropertyType(entity, relationFieldName);

      Object relationObj = relationType.getConstructor().newInstance();
      Object relationValue = PropertyUtils.getProperty(dto, relationFieldName);

      mapFieldsToTarget(relationValue, relationObj, selectedFieldPerClass.get(relationType));
      PropertyUtils.setProperty(entity, relationFieldName, relationObj);
    }

    // Map Custom Fields
    for (CustomFieldResolverSpec<D> cfr : entityResolvers) {
      String fieldName = cfr.getField();
      if (selectedFieldPerClass.get(dtoClass).contains(fieldName)) {
        PropertyUtils.setProperty(entity, fieldName, cfr.getResolver().apply(dto));
      }
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
