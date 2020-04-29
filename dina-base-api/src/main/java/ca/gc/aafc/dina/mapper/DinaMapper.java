package ca.gc.aafc.dina.mapper;

import java.util.Map;
import java.util.Set;

import org.apache.commons.beanutils.PropertyUtils;

import lombok.SneakyThrows;

public class DinaMapper<D, E> {

  private final Class<D> dtoClass;
  private final Class<E> entityClass;

  public DinaMapper(Class<D> dtoClass, Class<E> entityClass) {
    this.dtoClass = dtoClass;
    this.entityClass = entityClass;
  }

  @SneakyThrows
  public D toDto(E entity, Map<Class<?>, Set<String>> selectedFieldPerClass, Set<String> relations) {
    D dto = dtoClass.getConstructor().newInstance();

    // Map non relations
    mapFieldsToTarget(entity, dto, selectedFieldPerClass.get(entity.getClass()));

    // Map relations
    for (String relationFieldName : relations) {
      Class<?> relationDtoType = PropertyUtils.getPropertyType(dto, relationFieldName);

      Object relationDto = relationDtoType.getConstructor().newInstance();
      Object entityRelationField = PropertyUtils.getProperty(entity, relationFieldName);

      mapFieldsToTarget(entityRelationField, relationDto, selectedFieldPerClass.get(relationDtoType));
      PropertyUtils.setProperty(dto, relationFieldName, relationDto);
    }

    return dto;
  }

  @SneakyThrows
  private static <T, S> void mapFieldsToTarget(S source, T target, Set<String> selectedFieldPerClass) {
    for (String attribute : selectedFieldPerClass) {
      PropertyUtils.setProperty(target, attribute, PropertyUtils.getProperty(source, attribute));
    }
  }

}
