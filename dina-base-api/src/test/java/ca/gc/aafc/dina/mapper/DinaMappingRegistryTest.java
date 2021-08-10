package ca.gc.aafc.dina.mapper;

import ca.gc.aafc.dina.dto.DepartmentDto;
import ca.gc.aafc.dina.dto.EmployeeDto;
import ca.gc.aafc.dina.dto.ExternalRelationDto;
import ca.gc.aafc.dina.dto.PersonDTO;
import ca.gc.aafc.dina.dto.ProjectDTO;
import ca.gc.aafc.dina.dto.RelatedEntity;
import ca.gc.aafc.dina.dto.TaskDTO;
import ca.gc.aafc.dina.entity.Department;
import ca.gc.aafc.dina.entity.Employee;
import ca.gc.aafc.dina.entity.Person;
import ca.gc.aafc.dina.entity.Task;
import io.crnk.core.resource.annotations.JsonApiResource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.javers.core.metamodel.annotation.TypeName;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

public class DinaMappingRegistryTest {

  @Test
  void init_WhenInvalidGenericTypes_ThrowsIllegalStateException() {
    Assertions.assertThrows(
      IllegalStateException.class,
      () -> new DinaMappingRegistry(InvalidGenericDataTypeDto.class));
  }

  @Test
  void findJsonIdFieldName() {
    DinaMappingRegistry registry = new DinaMappingRegistry(ProjectDTO.class);
    Assertions.assertEquals("uuid", registry.findJsonIdFieldName(ProjectDTO.class));
    Assertions.assertEquals("uuid", registry.findJsonIdFieldName(TaskDTO.class));
    Assertions.assertEquals("id", registry.findJsonIdFieldName(ExternalRelationDto.class));
    Assertions.assertThrows(
      IllegalArgumentException.class, () -> registry.findJsonIdFieldName(Task.class));
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
    Assertions.assertEquals(DepartmentDto.class, resultRelation.getElementType());
    Assertions.assertFalse(resultRelation.isCollection());

    DinaMappingRegistry.InternalRelation resultCollectionRelation = results.stream()
      .filter(ir -> ir.getName().equals("departments")).findFirst().orElse(null);
    Assertions.assertNotNull(resultCollectionRelation);
    Assertions.assertEquals(DepartmentDto.class, resultCollectionRelation.getElementType());
    Assertions.assertTrue(resultCollectionRelation.isCollection());
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
    Assertions.assertEquals(Department.class, resultRelation.getElementType());
    Assertions.assertFalse(resultRelation.isCollection());

    DinaMappingRegistry.InternalRelation resultCollectionRelation = results.stream()
      .filter(ir -> ir.getName().equals("departments")).findFirst().orElse(null);
    Assertions.assertNotNull(resultCollectionRelation);
    Assertions.assertEquals(Department.class, resultCollectionRelation.getElementType());
    Assertions.assertTrue(resultCollectionRelation.isCollection());
  }

  @Test
  void findMappableRelationsForClass_NestedRelations_NestedRelationsFound() {
    DinaMappingRegistry registry = new DinaMappingRegistry(PersonDTO.class);

    Set<DinaMappingRegistry.InternalRelation> results = registry
      .findMappableRelationsForClass(Department.class);

    Assertions.assertEquals(1, results.size());

    DinaMappingRegistry.InternalRelation resultRelation = results.stream()
      .filter(ir -> ir.getName().equals("employees")).findFirst().orElse(null);
    Assertions.assertNotNull(resultRelation);
    Assertions.assertEquals(Employee.class, resultRelation.getElementType());
    Assertions.assertTrue(resultRelation.isCollection());
  }

  @Test
  void isRelationExternal() {
    DinaMappingRegistry registry = new DinaMappingRegistry(ProjectDTO.class);
    Assertions.assertTrue(registry.isRelationExternal("acMetaDataCreator"));
    Assertions.assertTrue(registry.isRelationExternal("originalAuthor"));
    Assertions.assertFalse(registry.isRelationExternal("task"));
  }

  @Test
  void findExternalType() {
    DinaMappingRegistry registry = new DinaMappingRegistry(ProjectDTO.class);
    Assertions.assertEquals("agent", registry.findExternalType("acMetaDataCreator"));
    Assertions.assertEquals("author", registry.findExternalType("originalAuthor"));
  }

  @Test
  void getExternalRelations() {
    DinaMappingRegistry registry = new DinaMappingRegistry(ProjectDTO.class);
    MatcherAssert.assertThat(
      registry.getExternalRelations(),
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
  @TypeName("invalid")
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

}
