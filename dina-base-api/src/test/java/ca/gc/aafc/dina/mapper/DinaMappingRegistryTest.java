package ca.gc.aafc.dina.mapper;

import ca.gc.aafc.dina.dto.DepartmentDto;
import ca.gc.aafc.dina.dto.ExternalRelationDto;
import ca.gc.aafc.dina.dto.PersonDTO;
import ca.gc.aafc.dina.dto.ProjectDTO;
import ca.gc.aafc.dina.dto.TaskDTO;
import ca.gc.aafc.dina.entity.ComplexObject;
import ca.gc.aafc.dina.entity.Department;
import ca.gc.aafc.dina.entity.Person;
import ca.gc.aafc.dina.entity.Task;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DinaMappingRegistryTest {

  @Test
  void findJsonIdFieldName() {
    DinaMappingRegistry registry = new DinaMappingRegistry(ProjectDTO.class);
    Assertions.assertEquals("uuid", registry.findJsonIdFieldName(ProjectDTO.class));
    Assertions.assertEquals("uuid", registry.findJsonIdFieldName(TaskDTO.class));
    Assertions.assertEquals("id", registry.findJsonIdFieldName(ExternalRelationDto.class));
    Assertions.assertNull(registry.findJsonIdFieldName(Task.class));
  }

  @Test
  void isRelationCollection() {
    DinaMappingRegistry registry = new DinaMappingRegistry(PersonDTO.class);
    Assertions.assertTrue(registry.isRelationCollection(PersonDTO.class, "departments"));
    Assertions.assertTrue(registry.isRelationCollection(Person.class, "departments"));
    Assertions.assertTrue(registry.isRelationCollection(DepartmentDto.class, "employees"));
    Assertions.assertTrue(registry.isRelationCollection(Department.class, "employees"));
    Assertions.assertFalse(registry.isRelationCollection(PersonDTO.class, "department"));
    Assertions.assertFalse(registry.isRelationCollection(Person.class, "department"));
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
      Matchers.containsInAnyOrder("acMetaDataCreator", "originalAuthor"));
  }

  @Test
  void getResolvedType() {
    ProjectDTO dto = ProjectDTO.builder().build();
    Assertions.assertEquals(
      ComplexObject.class, DinaMappingRegistry.getResolvedType(dto, "nameTranslations"));
    Assertions.assertEquals(TaskDTO.class, DinaMappingRegistry.getResolvedType(dto, "task"));
  }
}
