package ca.gc.aafc.dina.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.factory.Mappers;

import ca.gc.aafc.dina.dto.DepartmentDto;
import ca.gc.aafc.dina.dto.EmployeeDto;
import ca.gc.aafc.dina.dto.PersonDTO;
import ca.gc.aafc.dina.entity.Department;
import ca.gc.aafc.dina.entity.Employee;
import ca.gc.aafc.dina.entity.Person;

import java.util.Set;

@Mapper
public interface DepartmentMapper extends DinaMapperV2<DepartmentDto, Department> {

  DepartmentMapper INSTANCE = Mappers.getMapper(DepartmentMapper.class);

  @Mapping(target = "employeeCount", ignore = true)
  DepartmentDto toDto(Department entity, @Context Set<String> provided, @Context String scope);

  @Mapping(target = "employees", ignore = true)
  @Mapping(target = "departmentOwner", ignore = true)
  @Mapping(target = "departmentType", ignore = true)
  Department toEntity(DepartmentDto dto, @Context Set<String> provided, @Context String scope);

  @Mapping(target = "employees", ignore = true)
  @Mapping(target = "departmentHead", ignore = true)
  @Mapping(target = "departmentOwner", ignore = true)
  @Mapping(target = "departmentType", ignore = true)
  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  void patchEntity(@MappingTarget Department entity, DepartmentDto dto, @Context Set<String> provided, @Context String scope);

  @Mapping(target = "customField", ignore = true)
  EmployeeDto employeeToDto(Employee entity);

  @Mapping(target = "customField", ignore = true)
  Employee employeeToEntity(EmployeeDto dto);

  @Mapping(target = "department", ignore = true)
  PersonDTO personToDto(Person entity);

}
