package ca.gc.aafc.dina.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.factory.Mappers;

import ca.gc.aafc.dina.dto.CarDriverDto;
import ca.gc.aafc.dina.dto.JsonbCarDto;
import ca.gc.aafc.dina.entity.CarDriver;
import ca.gc.aafc.dina.entity.JsonbCar;

import java.util.Set;

@Mapper
public interface CarDriverMapper extends DinaMapperV2<CarDriverDto, CarDriver> {

  CarDriverMapper INSTANCE = Mappers.getMapper(CarDriverMapper.class);

  CarDriverDto toDto(CarDriver entity, @Context Set<String> provided, @Context String scope);
  CarDriver toEntity(CarDriverDto dto, @Context Set<String> provided, @Context String scope);

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  void patchEntity(@MappingTarget CarDriver entity, CarDriverDto dto, @Context Set<String> provided, @Context String scope);

  JsonbCarDto jsonbCarToDto(JsonbCar entity);

  JsonbCar jsonbCarToEntity(JsonbCarDto dto);
}
