package ca.gc.aafc.dina.mapper;

import ca.gc.aafc.dina.dto.ExternalRelationDto;
import ca.gc.aafc.dina.dto.PersonDTO;
import ca.gc.aafc.dina.dto.ProjectDTO;
import ca.gc.aafc.dina.dto.TaskDTO;
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
  void isCollection() {
    DinaMappingRegistry registry = new DinaMappingRegistry(PersonDTO.class);
    Assertions.assertTrue(registry.isCollection("departments"));
    Assertions.assertFalse(registry.isCollection("department"));
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
}
