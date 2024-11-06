package ca.gc.aafc.dina.mapper;

import java.util.Set;

import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import ca.gc.aafc.dina.dto.EmployeeDto;
import ca.gc.aafc.dina.dto.PersonDTO;
import ca.gc.aafc.dina.entity.Employee;
import ca.gc.aafc.dina.entity.Person;

@Mapper
public interface PersonMapper extends  DinaMapperV2<PersonDTO, Person> {

  PersonMapper INSTANCE = Mappers.getMapper(PersonMapper.class);

  PersonDTO toDto(Person entity, @Context Set<String> provided, @Context String scope);
  Person toEntity(PersonDTO dto, @Context Set<String> provided, @Context String scope);

  @Mapping(target = "customField", ignore = true)
  EmployeeDto employeeToDto(Employee entity);

  @Mapping(target = "customField", ignore = true)
  Employee employeeToEntity(EmployeeDto dto);

}
