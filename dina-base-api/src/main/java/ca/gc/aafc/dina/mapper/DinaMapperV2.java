package ca.gc.aafc.dina.mapper;

import java.util.Set;
import java.util.UUID;

import org.mapstruct.Condition;
import org.mapstruct.Context;
import org.mapstruct.SourcePropertyName;

import ca.gc.aafc.dina.dto.ExternalRelationDto;

/**
 * MapStruct based mapper that supports provided property to prevent lazy-loading to be triggered.
 */
public interface DinaMapperV2 {

  default UUID externalRelationToUUID(ExternalRelationDto er) {
    if (er == null) {
      return null;
    }
    return UUID.fromString(er.getId());
  }

  @Condition
  default boolean isPropertyProvided(@SourcePropertyName String sourcePropertyName,
                                     @Context Set<String> provided) {
    return provided.contains(sourcePropertyName);
  }
}
