package ca.gc.aafc.dina.mapper;


import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import ca.gc.aafc.dina.dto.ChainDto;

@Mapper
public interface DtoMapper {

  DtoMapper INSTANCE = Mappers.getMapper( DtoMapper.class );

  @Mapping(target = "chainTemplate", ignore = true)
  @Mapping(target = "agent", ignore = true)
  ChainDto toChainDto(Map<String, String> map);

  default UUID objToUUID(Object uuid) {
    return UUID.fromString(uuid.toString());
  }

  default OffsetDateTime strToOffsetDateTime(String str) {
    return OffsetDateTime.parse(str);
  }

}
