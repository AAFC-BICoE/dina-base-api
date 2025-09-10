package ca.gc.aafc.dina.mapper;

import org.apache.commons.lang3.StringUtils;
import org.mapstruct.Condition;
import org.mapstruct.Context;
import org.mapstruct.MappingTarget;
import org.mapstruct.SourcePropertyName;

import ca.gc.aafc.dina.dto.ExternalRelationDto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * MapStruct based mapper that supports provided property to prevent lazy-loading to be triggered.
 */
public interface DinaMapperV2<D, E> {

  /**
   * Map an entity to a new instance of a dto.
   * @param entity
   * @param provided provided properties so only those will be set
   * @param scope used to check provided properties within nested properties
   * @return dto instance
   */
  D toDto(E entity, @Context Set<String> provided, @Context String scope);

  /**
   * Map a dto to a new instance of an entity.
   * @param dto
   * @param provided provided properties so only those will be set
   * @param scope used to check provided properties within nested properties
   * @return entity instance
   */
  E toEntity(D dto, @Context Set<String> provided, @Context String scope);

  /**
   * Patch an existing entity from a DTO and a set of provided fields.
   * Always set @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
   * on the interface extending that one. Otherwise, non-provided values will be set to null.
   * @param entity entity instance to be patched
   * @param dto
   * @param provided provided properties so only those will be set
   * @param scope used to check provided properties within nested properties
   */
  void patchEntity(@MappingTarget E entity, D dto, @Context Set<String> provided, @Context String scope);

  /**
   * Used to map the uuid of an {@link ExternalRelationDto}.
   * To support legacy classes.
   * @param er
   * @return
   */
  default UUID externalRelationToUUID(ExternalRelationDto er) {
    if (er == null) {
      return null;
    }
    return UUID.fromString(er.getId());
  }

  /**
   * Used by MapStruct to map String arrays.
   * @param arr
   * @return
   */
  default String[] nullSafeStringArrayCopy(String[] arr) {
    if (arr == null) {
      return null;
    }
    return Arrays.copyOf(arr, arr.length);
  }

  /**
   * Used by MapStruct to map lists.
   * Note: will always create ArrayList
   * @return
   */
  default <T> List<T> nullSafeList(List<T> list) {
    if (list == null) {
      return null;
    }
    return new ArrayList<>(list);
  }

  /**
   * Used by MapStruct to handle null values in maps.
   * Note: will always create an linked hash map
   */
  default <K, V> Map<K, V> nullSafeMap(Map<K, V> map) {
    if (map == null) {
      return null;
    }
    return new LinkedHashMap<>(map);
  }

  /**
   * Used by MapStruct to control which field to set.
   *
   * @param sourcePropertyName
   * @param provided
   * @param scope              used to check provided properties within nested properties
   * @return
   */
  @Condition
  default boolean isPropertyProvided(@SourcePropertyName String sourcePropertyName,
                                     @Context Set<String> provided, @Context String scope) {
    if (StringUtils.isBlank(scope)) {
      return provided.contains(sourcePropertyName);
    }
    // dealing with nested objects
    return provided.contains(scope + "." + sourcePropertyName);
  }

  /**
   * Used by MapStruct to control which field to set with specific scope.
   *
   * @param sourcePropertyName
   * @param provided
   * @param scope              used to check provided properties within nested properties
   * @return
   */
  @Condition
  default boolean isPropertyProvidedInScope(@SourcePropertyName String sourcePropertyName, Set<String> provided, String scope) {
    return StringUtils.isBlank(scope) ? provided.contains(sourcePropertyName) : provided.contains(scope + "." + sourcePropertyName);
  }
}
