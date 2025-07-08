package ca.gc.aafc.dina.mapper;

import java.util.Set;

import org.mapstruct.BeanMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.factory.Mappers;

import ca.gc.aafc.dina.dto.EmployeeDto;
import ca.gc.aafc.dina.entity.CarDriver;
import ca.gc.aafc.dina.entity.Employee;
import ca.gc.aafc.dina.entity.JsonbCar;
import ca.gc.aafc.dina.filter.SimpleFilterResolverJsonbV2IT;

@Mapper
public interface CarDriverMapper extends DinaMapperV2<SimpleFilterResolverJsonbV2IT.DinaFilterResolverJsonbITConfig.CarDriverDto, CarDriver> {

  CarDriverMapper INSTANCE = Mappers.getMapper(CarDriverMapper.class);

  SimpleFilterResolverJsonbV2IT.DinaFilterResolverJsonbITConfig.CarDriverDto toDto(CarDriver entity, @Context Set<String> provided, @Context String scope);
  CarDriver toEntity(SimpleFilterResolverJsonbV2IT.DinaFilterResolverJsonbITConfig.CarDriverDto dto, @Context Set<String> provided, @Context String scope);

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  void patchEntity(@MappingTarget CarDriver entity, SimpleFilterResolverJsonbV2IT.DinaFilterResolverJsonbITConfig.CarDriverDto dto, @Context Set<String> provided, @Context String scope);

  SimpleFilterResolverJsonbV2IT.DinaFilterResolverJsonbITConfig.JsonbCarDto jsonbCarToDto(JsonbCar entity);

  JsonbCar jsonbCarToEntity(SimpleFilterResolverJsonbV2IT.DinaFilterResolverJsonbITConfig.JsonbCarDto dto);
}
