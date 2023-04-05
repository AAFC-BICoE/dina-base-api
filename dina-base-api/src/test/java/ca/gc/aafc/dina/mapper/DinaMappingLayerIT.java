package ca.gc.aafc.dina.mapper;

import ca.gc.aafc.dina.BasePostgresItContext;
import ca.gc.aafc.dina.ExternalResourceProviderImplementation;
import ca.gc.aafc.dina.DinaUserConfig.DepartmentDinaService;
import ca.gc.aafc.dina.dto.DepartmentDto;
import ca.gc.aafc.dina.dto.ExternalRelationDto;
import ca.gc.aafc.dina.dto.PersonDTO;
import ca.gc.aafc.dina.dto.ProjectDTO;
import ca.gc.aafc.dina.dto.TaskDTO;
import ca.gc.aafc.dina.entity.Department;
import ca.gc.aafc.dina.entity.Employee;
import ca.gc.aafc.dina.entity.Person;
import ca.gc.aafc.dina.entity.Project;
import ca.gc.aafc.dina.entity.Task;
import ca.gc.aafc.dina.jpa.BaseDAO;
import ca.gc.aafc.dina.repository.DinaRepository;
import ca.gc.aafc.dina.repository.external.ExternalResourceProvider;
import ca.gc.aafc.dina.security.AllowAllAuthorizationService;
import ca.gc.aafc.dina.service.DefaultDinaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.crnk.core.queryspec.PathSpec;
import io.crnk.core.queryspec.QuerySpec;
import lombok.NonNull;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.springframework.validation.SmartValidator;

import javax.inject.Inject;
import javax.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Transactional
public class DinaMappingLayerIT extends BasePostgresItContext {

  @Inject
  private DefaultDinaService<Task> service;

  @Inject
  private DefaultDinaService<Person> personDefaultDinaService;

  @Inject 
  private DepartmentDinaService departmentService;

  @Inject
  private BaseDAO baseDAO;

  private DinaMappingLayer<ProjectDTO, Project> mappingLayer;

  @BeforeEach
  void setUp() {
    mappingLayer = new DinaMappingLayer<>(ProjectDTO.class, service, new DinaMapper<>(ProjectDTO.class));
  }

  @Test
  void mapEntitiesToDto_WhenNoRelationsIncluded_ExternalRelationsMapped() {
    Project entity1 = newProject();
    Project entity2 = newProject();

    List<ProjectDTO> results = mappingLayer.mapEntitiesToDto(
      new QuerySpec(ProjectDTO.class), Arrays.asList(entity1, entity2));

    assertProject(entity1, results.get(0));
    assertProject(entity2, results.get(1));
  }

  @Test
  void mapEntitiesToDto_WhenNoRelationsIncluded_ShallowIdsMapped() {
    Task expectedTask = newTask();

    Project entity1 = newProject();
    entity1.setTask(expectedTask);
    Project entity2 = newProject();

    List<ProjectDTO> results = mappingLayer.mapEntitiesToDto(
      new QuerySpec(ProjectDTO.class), Arrays.asList(entity1, entity2));

    Assertions.assertEquals(expectedTask.getUuid(), results.get(0).getTask().getUuid(),
      "Shallow id should of been mapped");
    Assertions.assertEquals(
      0, results.get(0).getTask().getPowerLevel(),
      "Shallow relation should not map an attribute");
    Assertions.assertNull(
      results.get(1).getTask(),
      "Null Relation should map as null");
  }

  @Test
  void mapEntitiesToDto_lazyLoadedRelationshipsNotIncluded_notMapped() {
    DinaMappingLayer<DepartmentDto, Department> departmentMappingLayer = new DinaMappingLayer<>(
        DepartmentDto.class, departmentService, new DinaMapper<>(DepartmentDto.class));

    // Create the department.
    Department dep1 = Department.builder()
        .uuid(UUID.randomUUID())
        .name("Dunder Mifflin Paper Company, Inc.")
        .location("Scranton")
        .build();
    baseDAO.create(dep1, true);

    // Create the employee, associated with a department.
    Employee emp1 = Employee.builder()
        .uuid(UUID.randomUUID())
        .name("Dwight Schrute")
        .department(dep1)
        .build();
    baseDAO.create(emp1, true);

    // Detach the objects to force a reload.
    baseDAO.detach(emp1);
    baseDAO.detach(dep1);

    // Load the entity from the database.
    Department retrievedDepartment = baseDAO.findOneByNaturalId(dep1.getUuid(), Department.class);
    Assertions.assertNotNull(retrievedDepartment);
    Assertions.assertFalse(baseDAO.isLoaded(retrievedDepartment, "employees"),
        "The employees relationship should not be loaded in at this point.");

    // Map department to DTO without loading employees relationship
    DepartmentDto departmentDto1 = departmentMappingLayer.mapEntitiesToDto(
        new QuerySpec(DepartmentDto.class), Arrays.asList(retrievedDepartment)).get(0);
    Assertions.assertNull(departmentDto1.getEmployees(),
        "Employees has not been lazy-loaded in, so it should not be mapped.");

    // Force lazy-load the employees relationship.
    Employee retrievedEmployee = retrievedDepartment.getEmployees().get(0);
    Assertions.assertNotNull(retrievedEmployee);
    Assertions.assertTrue(baseDAO.isLoaded(retrievedDepartment, "employees"),
        "The employees relationship should be loaded in at this point.");

    // Map department to DTO again and check employees relationship is mapped.
    DepartmentDto departmentDto2 = departmentMappingLayer.mapEntitiesToDto(
        new QuerySpec(DepartmentDto.class), Arrays.asList(retrievedDepartment)).get(0);
    Assertions.assertNotNull(departmentDto2.getEmployees().get(0),
        "Employees has been lazy-loaded in, so it should be mapped.");
  }

  @Test
  void mapEntitiesToDto_WhenRelationIncluded_RelationFullyMapped() {
    Person randomPerson = persistPerson();
    Task expectedTask = newTask();

    Project entity1 = newProject();
    entity1.setTask(expectedTask);
    entity1.setRandomPeople(List.of(randomPerson));
    Project entity2 = newProject();

    QuerySpec query = new QuerySpec(ProjectDTO.class);
    query.includeRelation(PathSpec.of("task"));
    query.includeRelation(PathSpec.of("randomPeople"));
    List<ProjectDTO> results = mappingLayer.mapEntitiesToDto(
      query,
      Arrays.asList(entity1, entity2));

    assertProject(entity1, results.get(0));
    assertProject(entity2, results.get(1));
    Assertions.assertEquals(expectedTask.getUuid(), results.get(0).getTask().getUuid());
    Assertions.assertEquals(expectedTask.getPowerLevel(), results.get(0).getTask().getPowerLevel());
    Assertions.assertNull(results.get(1).getTask());
    Assertions.assertEquals(
      entity1.getRandomPeople().get(0).getName(),
      results.get(0).getRandomPeople().get(0).getName());
  }

  @Test
  void mapEntitiesToDto_WhenExternalRelationNull_NullMapped() {
    Project entity1 = newProject();
    entity1.setAcMetaDataCreator(null);

    List<ProjectDTO> results = mappingLayer.mapEntitiesToDto(
      new QuerySpec(ProjectDTO.class), Collections.singletonList(entity1));

    Assertions.assertNull(results.get(0).getAcMetaDataCreator());
  }

  @Test
  void mapToEntity_WhenRelationsNull_NullsMapped() {
    ProjectDTO dto = newProjectDto();

    Project result = new Project();
    mappingLayer.mapToEntity(dto, result);

    validateProjectAttributes(dto, result);
    Assertions.assertNull(result.getAcMetaDataCreator());
    Assertions.assertNull(result.getOriginalAuthor());
    Assertions.assertNull(result.getTask());
  }

  @Test
  void mapToEntity_WithRelations_RelationsMapped() {
    Person randomPerson = persistPerson();
    Task persistedTask = service.create(newTask());

    ProjectDTO dto = newProjectDto();
    dto.setTask(TaskDTO.builder().uuid(persistedTask.getUuid()).build());
    dto.setAcMetaDataCreator(ExternalRelationDto.builder().id(UUID.randomUUID().toString())
      .build());
    dto.setOriginalAuthor(ExternalRelationDto.builder().id(UUID.randomUUID().toString()).build());
    dto.setRandomPeople(List.of(PersonDTO.builder().uuid(randomPerson.getUuid()).build()));

    Project result = new Project();
    mappingLayer.mapToEntity(dto, result);

    validateProjectAttributes(dto, result);
    // Validate External Relation
    Assertions.assertEquals(
      dto.getAcMetaDataCreator().getId(), result.getAcMetaDataCreator().toString());
    Assertions.assertEquals(
      dto.getOriginalAuthor().getId(), result.getOriginalAuthor().toString());
    // Validate internal relations
    Assertions.assertEquals(persistedTask.getUuid(), result.getTask().getUuid());
    Assertions.assertEquals(persistedTask.getPowerLevel(), result.getTask().getPowerLevel(),
      "Internal Relation should of been linked");
    Assertions.assertEquals(
      dto.getRandomPeople().get(0).getUuid(),
      result.getRandomPeople().get(0).getUuid());
  }

  private static void validateProjectAttributes(ProjectDTO dto, Project result) {
    Assertions.assertEquals(dto.getName(), result.getName());
    Assertions.assertEquals(dto.getUuid(), result.getUuid());
    Assertions.assertEquals(dto.getCreatedBy(), result.getCreatedBy());
    Assertions.assertTrue(dto.getCreatedOn().isEqual(result.getCreatedOn()));
  }

  private void assertProject(Project entity, ProjectDTO result) {
    // Validate attributes
    Assertions.assertEquals(entity.getName(), result.getName());
    Assertions.assertEquals(entity.getUuid(), result.getUuid());
    Assertions.assertEquals(entity.getCreatedBy(), result.getCreatedBy());
    Assertions.assertTrue(entity.getCreatedOn().isEqual(result.getCreatedOn()));
    // Validate External Relation
    Assertions.assertEquals(
      entity.getAcMetaDataCreator().toString(), result.getAcMetaDataCreator().getId());
    Assertions.assertEquals(
      entity.getOriginalAuthor().toString(), result.getOriginalAuthor().getId());
    Assertions.assertEquals(
      entity.getAuthors().get(0).toString(), result.getAuthors().get(0).getId());
  }

  private ProjectDTO newProjectDto() {
    return ProjectDTO.builder()
      .uuid(UUID.randomUUID())
      .createdBy(RandomStringUtils.randomAlphabetic(5))
      .createdOn(OffsetDateTime.now())
      .name(RandomStringUtils.randomAlphabetic(5))
      .authors(List.of(ExternalRelationDto.builder()
        .id(UUID.randomUUID().toString()).type("authors")
        .build()))
      .build();
  }

  private static Project newProject() {
    return Project.builder()
      .uuid(UUID.randomUUID())
      .createdBy(RandomStringUtils.randomAlphabetic(5))
      .createdOn(OffsetDateTime.now())
      .name(RandomStringUtils.randomAlphabetic(5))
      .acMetaDataCreator(UUID.randomUUID())
      .originalAuthor(UUID.randomUUID())
      .authors(List.of(UUID.randomUUID()))
      .build();
  }

  private static Task newTask() {
    return Task.builder()
      .powerLevel(RandomUtils.nextInt())
      .uuid(UUID.randomUUID())
      .build();
  }

  private Person persistPerson() {
    return personDefaultDinaService.create(Person.builder()
      .uuid(UUID.randomUUID())
      .name(RandomStringUtils.randomAlphabetic(4))
      .build());
  }

  @TestConfiguration
  @Import(ExternalResourceProviderImplementation.class)
  static class DinaMappingLayerITITConfig {
    @Bean
    public DinaRepository<ProjectDTO, Project> projectRepo(
      ExternalResourceProvider externalResourceProvider,
      BuildProperties buildProperties,
      ProjectDinaService projectDinaService, ObjectMapper objMapper
    ) {
      return new DinaRepository<>(
        projectDinaService,
        new AllowAllAuthorizationService(),
        Optional.empty(),
        new DinaMapper<>(ProjectDTO.class),
        ProjectDTO.class,
        Project.class,
        null,
        externalResourceProvider,
        buildProperties, objMapper
      );
    }

    @Bean
    public DinaRepository<TaskDTO, Task> taskRepo(
      ExternalResourceProvider externalResourceProvider,
      BuildProperties buildProperties,
      TaskDinaService taskDinaService, ObjectMapper objMapper
    ) {
      return new DinaRepository<>(
        taskDinaService,
        new AllowAllAuthorizationService(),
        Optional.empty(),
        new DinaMapper<>(TaskDTO.class),
        TaskDTO.class,
        Task.class,
        null,
        externalResourceProvider,
        buildProperties, objMapper
      );
    }

    @Service
    static class ProjectDinaService extends DefaultDinaService<Project> {
  
      public ProjectDinaService(@NonNull BaseDAO baseDAO, SmartValidator sv) {
        super(baseDAO, sv);
      }
    }
  
    
    @Service
    static class TaskDinaService extends DefaultDinaService<Task> {
  
      public TaskDinaService(@NonNull BaseDAO baseDAO, SmartValidator sv) {
        super(baseDAO, sv);
      }
    }
  }

}
