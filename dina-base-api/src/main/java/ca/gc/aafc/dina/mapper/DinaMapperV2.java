package ca.gc.aafc.dina.mapper;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

import org.mapstruct.Condition;
import org.mapstruct.Context;
import org.mapstruct.Named;
import org.mapstruct.SourcePropertyName;

import ca.gc.aafc.dina.dto.ExternalRelationDto;

/**
 * MapStruct based mapper that supports provided property to prevent lazy-loading to be triggered.
 */
public interface DinaMapperV2<D, E> {

  /**
   * Map an entity to a new instance of a dto.
   * @param entity
   * @param provided provided properties so only those will be set
   * @return dto instance
   */
  D toDto(E entity, @Context Set<String> provided);

  /**
   * Map a dto to a new instance of an entity.
   * @param dto
   * @param provided provided properties so only those will be set
   * @return entity instance
   */
  E toEntity(D dto, @Context Set<String> provided);


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
   * Used by MapStruct to control which field to set.
   * @param sourcePropertyName
   * @param provided
   * @return
   */
  @Condition
  default boolean isPropertyProvided(@SourcePropertyName String sourcePropertyName,
                                     @Context Set<String> provided) {
    return provided.contains(sourcePropertyName);
  }

  /**
   * Used to map UUID of type person to {@link ExternalRelationDto} for all legacy classes.
   * Usage: @Mapping(source = "agent", target = "agent", qualifiedByName = "uuidToPersonExternalRelation")
   * @param personUUID
   * @return
   */
  @Named("uuidToPersonExternalRelation")
  static ExternalRelationDto uuidToPersonExternalRelation(UUID personUUID) {
    if (personUUID == null) {
      return null;
    }
    return ExternalRelationDto.builder().id(personUUID.toString()).type("person").build();
  }
}
