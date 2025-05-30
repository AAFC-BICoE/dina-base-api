package ca.gc.aafc.dina.mapper;

import ca.gc.aafc.dina.dto.DepartmentDto;
import ca.gc.aafc.dina.dto.EmployeeDto;
import ca.gc.aafc.dina.dto.InheritedDto;
import ca.gc.aafc.dina.dto.PersonDTO;
import ca.gc.aafc.dina.dto.ProjectDTO;
import ca.gc.aafc.dina.dto.RelatedEntity;
import ca.gc.aafc.dina.dto.TaskDTO;
import ca.gc.aafc.dina.entity.Department;
import ca.gc.aafc.dina.entity.Employee;
import ca.gc.aafc.dina.entity.Inherited;
import ca.gc.aafc.dina.entity.Person;
import io.crnk.core.resource.annotations.JsonApiResource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class DinaMappingRegistryTest {

  @Test
  void init_whenNonMatchingDataTypes_ThrowsIllegalStateException() {
    Assertions.assertThrows( // Invalid data type
      IllegalStateException.class,
      () -> new DinaMappingRegistry(NonMatchingDataTypeDto.class));
    Assertions.assertThrows( // Invalid generic type
      IllegalStateException.class,
      () -> new DinaMappingRegistry(InvalidGenericDataTypeDto.class));
  }

  @Test
  void init_whenNonMatchingDataTypesWithFlag_NoException() {
    Assertions.assertDoesNotThrow(() -> new DinaMappingRegistry(NonMatchingDataTypeDto.class, true));
    Assertions.assertDoesNotThrow(
      () -> new DinaMappingRegistry(InvalidGenericDataTypeDto.class, true));
  }

  @Test
  void findJsonIdFieldName() {
    DinaMappingRegistry registry = new DinaMappingRegistry(ProjectDTO.class);
    Assertions.assertEquals("uuid", registry.findJsonIdFieldName(ProjectDTO.class));
    Assertions.assertEquals("uuid", registry.findJsonIdFieldName(TaskDTO.class));
  }

  @Test
  void getAttributePerClass_WhenAttributesInherited() {
    DinaMappingRegistry registry = new DinaMappingRegistry(InheritedDto.class);
    String expectedField = "Inherited";
    assertTrue(registry.getAttributesForClass(InheritedDto.class).contains(expectedField));
    assertTrue(registry.getAttributesForClass(Inherited.class).contains(expectedField));
  }

  @Test
  void findMappableRelationsForClass_DtoClass_MappableRelationsFound() {
    DinaMappingRegistry registry = new DinaMappingRegistry(PersonDTO.class);

    Set<DinaMappingRegistry.InternalRelation> results = registry
      .findMappableRelationsForClass(PersonDTO.class);

    Assertions.assertEquals(2, results.size());

    DinaMappingRegistry.InternalRelation resultRelation = results.stream()
      .filter(ir -> ir.getName().equals("department")).findFirst().orElse(null);
    Assertions.assertNotNull(resultRelation);
    Assertions.assertEquals(DepartmentDto.class, resultRelation.getDtoType());
    Assertions.assertFalse(resultRelation.isCollection());

    DinaMappingRegistry.InternalRelation resultCollectionRelation = results.stream()
      .filter(ir -> ir.getName().equals("departmentsHeadBackup")).findFirst().orElse(null);
    Assertions.assertNotNull(resultCollectionRelation);
    Assertions.assertEquals(DepartmentDto.class, resultCollectionRelation.getDtoType());
    assertTrue(resultCollectionRelation.isCollection());
  }

  @Test
  void findMappableRelationsForClass_EntityClass_MappableRelationsFound() {
    DinaMappingRegistry registry = new DinaMappingRegistry(PersonDTO.class);

    Set<DinaMappingRegistry.InternalRelation> results = registry
      .findMappableRelationsForClass(Person.class);

    Assertions.assertEquals(2, results.size());

    DinaMappingRegistry.InternalRelation resultRelation = results.stream()
      .filter(ir -> ir.getName().equals("department")).findFirst().orElse(null);
    Assertions.assertNotNull(resultRelation);
    Assertions.assertEquals(DepartmentDto.class, resultRelation.getDtoType());
    Assertions.assertEquals(Department.class, resultRelation.getEntityType());
    Assertions.assertFalse(resultRelation.isCollection());

    DinaMappingRegistry.InternalRelation resultCollectionRelation = results.stream()
      .filter(ir -> ir.getName().equals("departmentsHeadBackup")).findFirst().orElse(null);
    Assertions.assertNotNull(resultCollectionRelation);
    Assertions.assertEquals(DepartmentDto.class, resultCollectionRelation.getDtoType());
    Assertions.assertEquals(Department.class, resultCollectionRelation.getEntityType());
    assertTrue(resultCollectionRelation.isCollection());
  }

  @Test
  void findMappableRelationsForClass_NestedRelations_NestedRelationsFound() {
    DinaMappingRegistry registry = new DinaMappingRegistry(PersonDTO.class);

    Set<DinaMappingRegistry.InternalRelation> results = registry
      .findMappableRelationsForClass(Department.class);

    // Employee and person should be found.
    Assertions.assertEquals(2, results.size());

    DinaMappingRegistry.InternalRelation employeesRelation = results.stream()
      .filter(ir -> ir.getName().equals("employees")).findFirst().orElse(null);
    Assertions.assertNotNull(employeesRelation);
    Assertions.assertEquals(Employee.class, employeesRelation.getEntityType());
    Assertions.assertEquals(EmployeeDto.class, employeesRelation.getDtoType());
    assertTrue(employeesRelation.isCollection());

    DinaMappingRegistry.InternalRelation personRelation = results.stream()
      .filter(ir -> ir.getName().equals("departmentOwner")).findFirst().orElse(null);
    Assertions.assertNotNull(personRelation);
    Assertions.assertEquals(Person.class, personRelation.getEntityType());
    Assertions.assertEquals(PersonDTO.class, personRelation.getDtoType());
    Assertions.assertFalse(personRelation.isCollection());
  }

  @Test
  void isRelationExternal() {
    DinaMappingRegistry registry = new DinaMappingRegistry(ProjectDTO.class);
    assertTrue(registry.isRelationExternal(ProjectDTO.class, "acMetaDataCreator"));
    assertTrue(registry.isRelationExternal(ProjectDTO.class, "originalAuthor"));
    Assertions.assertFalse(registry.isRelationExternal(ProjectDTO.class, "task"));
  }

  @Test
  void findExternalType() {
    DinaMappingRegistry registry = new DinaMappingRegistry(ProjectDTO.class);
    Assertions.assertEquals("agent", registry.findExternalType(ProjectDTO.class, "acMetaDataCreator"));
    Assertions.assertEquals("author", registry.findExternalType(ProjectDTO.class, "originalAuthor"));
  }

  @Test
  void getExternalRelations() {
    DinaMappingRegistry registry = new DinaMappingRegistry(ProjectDTO.class);
    MatcherAssert.assertThat(
      registry.getExternalRelations(ProjectDTO.class),
      Matchers.containsInAnyOrder("acMetaDataCreator", "originalAuthor", "authors"));
  }

  @Test
  void findNestedResource() {
    DinaMappingRegistry registry = new DinaMappingRegistry(PersonDTO.class);
    Assertions.assertEquals(
      PersonDTO.class,
      registry.resolveNestedResourceFromPath(PersonDTO.class, List.of("name")));
    Assertions.assertEquals(
      DepartmentDto.class,
      registry.resolveNestedResourceFromPath(PersonDTO.class, List.of("department")));
    Assertions.assertEquals(
      EmployeeDto.class,
      registry.resolveNestedResourceFromPath(PersonDTO.class, List.of("department", "employees")));
    Assertions.assertEquals(
      EmployeeDto.class,
      registry.resolveNestedResourceFromPath(PersonDTO.class, List.of("department", "employees", "job")));
  }

  @Data
  @JsonApiResource(type = ProjectDTO.RESOURCE_TYPE)
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @RelatedEntity(InvalidGenericDataTypeEntity.class)
  static class InvalidGenericDataTypeDto {
    private List<String> invalidList;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  static class InvalidGenericDataTypeEntity {
    private List<Integer> invalidList;
  }

  @Data
  @JsonApiResource(type = ProjectDTO.RESOURCE_TYPE)
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @RelatedEntity(NonMatchingDataTypeEntity.class)
  static class NonMatchingDataTypeDto {
    private String invalidList;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  static class NonMatchingDataTypeEntity {
    private Integer invalidList;
  }

}
