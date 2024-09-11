package ca.gc.aafc.dina.mapper;


import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import ca.gc.aafc.dina.dto.ChainDto;
import ca.gc.aafc.dina.entity.Chain;

import java.util.Set;

@Mapper
public interface DtoMapper extends DinaMapperV2 {

  DtoMapper INSTANCE = Mappers.getMapper( DtoMapper.class );

  Chain toChain(ChainDto dto, @Context Set<String> provided);
}
