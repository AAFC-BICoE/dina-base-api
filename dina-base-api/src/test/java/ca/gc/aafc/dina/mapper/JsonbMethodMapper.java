package ca.gc.aafc.dina.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import ca.gc.aafc.dina.dto.JsonbMethodDto;
import ca.gc.aafc.dina.entity.JsonbMethod;

@Mapper
public interface JsonbMethodMapper extends DinaMapperV2<JsonbMethodDto, JsonbMethod> {

  JsonbMethodMapper INSTANCE = Mappers.getMapper(JsonbMethodMapper.class);
}


