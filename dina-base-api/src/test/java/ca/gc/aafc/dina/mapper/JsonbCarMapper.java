package ca.gc.aafc.dina.mapper;

import java.util.Set;

import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import ca.gc.aafc.dina.dto.JsonbCarDto;
import ca.gc.aafc.dina.entity.JsonbCar;

@Mapper
public interface JsonbCarMapper extends DinaMapperV2<JsonbCarDto, JsonbCar> {

  JsonbCarMapper INSTANCE = Mappers.getMapper(JsonbCarMapper.class);

  JsonbCarDto toDto(JsonbCar entity, @Context Set<String> provided, @Context String scope);

  JsonbCar toEntity(JsonbCarDto dto, @Context Set<String> provided, @Context String scope);
}
