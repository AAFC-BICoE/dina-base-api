package ca.gc.aafc.dina.mapper;

import ca.gc.aafc.dina.ExternalResourceProviderImplementation;
import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.dto.ExternalRelationDto;
import ca.gc.aafc.dina.dto.ProjectDTO;
import ca.gc.aafc.dina.dto.TaskDTO;
import ca.gc.aafc.dina.entity.Project;
import ca.gc.aafc.dina.entity.Task;
import ca.gc.aafc.dina.jpa.BaseDAO;
import ca.gc.aafc.dina.repository.DinaRepository;
import ca.gc.aafc.dina.repository.external.ExternalResourceProvider;
import ca.gc.aafc.dina.service.DefaultDinaService;
import io.crnk.core.queryspec.PathSpec;
import io.crnk.core.queryspec.QuerySpec;
import lombok.NonNull;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.context.SpringBootTest;
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
@SpringBootTest(classes = TestDinaBaseApp.class)
public class DinaMappingLayerIT {

  @Inject
  private DefaultDinaService<Task> service;

  private DinaMappingLayer<ProjectDTO, Project> mappingLayer;

  private Task persistedTask;

  @BeforeEach
  void setUp() {
    mappingLayer = new DinaMappingLayer<>(
      ProjectDTO.class, service, new DinaMapper<>(ProjectDTO.class));
    persistedTask = Task.builder()
      .uuid(UUID.randomUUID())
      .powerLevel(RandomUtils.nextInt())
      .build();
    service.create(persistedTask);
    Assertions.assertNotNull(service.findOne(persistedTask.getUuid(), Task.class));
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
  void mapEntitiesToDto_WhenRelationIncluded_RelationFullyMapped() {
    Task expectedTask = newTask();

    Project entity1 = newProject();
    entity1.setTask(expectedTask);
    Project entity2 = newProject();

    QuerySpec query = new QuerySpec(ProjectDTO.class);
    query.includeRelation(PathSpec.of("task"));
    List<ProjectDTO> results = mappingLayer.mapEntitiesToDto(
      query,
      Arrays.asList(entity1, entity2));

    assertProject(entity1, results.get(0));
    assertProject(entity2, results.get(1));
    Assertions.assertEquals(expectedTask.getUuid(), results.get(0).getTask().getUuid());
    Assertions.assertEquals(expectedTask.getPowerLevel(), results.get(0).getTask().getPowerLevel());
    Assertions.assertNull(results.get(1).getTask());
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
    ProjectDTO dto = newProjectDto();
    dto.setTask(TaskDTO.builder().uuid(persistedTask.getUuid()).build());
    dto.setAcMetaDataCreator(ExternalRelationDto.builder().id(UUID.randomUUID().toString())
      .build());
    dto.setOriginalAuthor(ExternalRelationDto.builder().id(UUID.randomUUID().toString()).build());

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

  @TestConfiguration
  @Import(ExternalResourceProviderImplementation.class)
  static class DinaMappingLayerITITConfig {
    @Bean
    public DinaRepository<ProjectDTO, Project> projectRepo(
      BaseDAO baseDAO,
      ExternalResourceProvider externalResourceProvider,
      BuildProperties buildProperties,
      ProjectDinaService projectDinaService
    ) {
      return new DinaRepository<>(
        projectDinaService,
        null,
        Optional.empty(),
        new DinaMapper<>(ProjectDTO.class),
        ProjectDTO.class,
        Project.class,
        null,
        externalResourceProvider,
        buildProperties
      );
    }

    @Bean
    public DinaRepository<TaskDTO, Task> taskRepo(
      BaseDAO baseDAO,
      ExternalResourceProvider externalResourceProvider,
      BuildProperties buildProperties,
      TaskDinaService taskDinaService
    ) {
      return new DinaRepository<>(
        taskDinaService,
        null,
        Optional.empty(),
        new DinaMapper<>(TaskDTO.class),
        TaskDTO.class,
        Task.class,
        null,
        externalResourceProvider,
        buildProperties
      );
    }

    @Service
    class ProjectDinaService extends DefaultDinaService<Project> {
  
      public ProjectDinaService(@NonNull BaseDAO baseDAO, SmartValidator sv) {
        super(baseDAO, sv);
      }
    }
  
    
    @Service
    class TaskDinaService extends DefaultDinaService<Task> {
  
      public TaskDinaService(@NonNull BaseDAO baseDAO, SmartValidator sv) {
        super(baseDAO, sv);
      }
    }
  }

}
