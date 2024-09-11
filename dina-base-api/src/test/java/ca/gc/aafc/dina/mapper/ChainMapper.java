package ca.gc.aafc.dina.mapper;

import java.util.Set;

import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import ca.gc.aafc.dina.dto.ChainDto;
import ca.gc.aafc.dina.entity.Chain;

@Mapper
public interface ChainMapper extends DinaMapperV2<ChainDto, Chain> {

  ChainMapper INSTANCE = Mappers.getMapper( ChainMapper.class );

  @Mapping(source = "agent", target = "agent", qualifiedByName = "uuidToPersonExternalRelation")
  ChainDto toDto(Chain entity, @Context Set<String> provided);

}
