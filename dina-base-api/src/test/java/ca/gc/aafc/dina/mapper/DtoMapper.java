package ca.gc.aafc.dina.mapper;


import org.mapstruct.Condition;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.SourcePropertyName;
import org.mapstruct.factory.Mappers;

import ca.gc.aafc.dina.dto.ChainDto;
import ca.gc.aafc.dina.dto.ExternalRelationDto;
import ca.gc.aafc.dina.entity.Chain;

import java.util.Set;
import java.util.UUID;

@Mapper
public interface DtoMapper {

  DtoMapper INSTANCE = Mappers.getMapper( DtoMapper.class );

  Chain toChain(ChainDto dto, @Context Set<String> provided);

  default UUID externalRelationToUUID(ExternalRelationDto er) {
    if (er == null) {
      return null;
    }
    return UUID.fromString(er.getId());
  }

  @Condition
  default boolean isProvided(
    @SourcePropertyName String sourcePropertyName,
    @Context Set<String> provided) {
    return provided.contains(sourcePropertyName);
  }

}
