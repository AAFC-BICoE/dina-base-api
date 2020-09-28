package ca.gc.aafc.dina.jsonapi;

import ca.gc.aafc.dina.dto.RelatedEntity;
import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.filter.DinaFilterResolver;
import ca.gc.aafc.dina.jpa.BaseDAO;
import ca.gc.aafc.dina.mapper.DinaMapper;
import ca.gc.aafc.dina.metaInfo.JsonApiExternalRelation;
import ca.gc.aafc.dina.repository.DinaRepository;
import ca.gc.aafc.dina.service.DinaService;
import ca.gc.aafc.dina.testsupport.BaseRestAssuredTest;
import com.google.common.collect.ImmutableMap;
import io.crnk.client.CrnkClient;
import io.crnk.client.http.inmemory.InMemoryHttpAdapter;
import io.crnk.core.boot.CrnkBoot;
import io.crnk.core.engine.http.HttpMethod;
import io.crnk.core.queryspec.PathSpec;
import io.crnk.core.queryspec.QuerySpec;
import io.crnk.core.resource.annotations.JsonApiId;
import io.crnk.core.resource.annotations.JsonApiRelation;
import io.crnk.core.resource.annotations.JsonApiResource;
import io.crnk.operations.client.OperationsCall;
import io.crnk.operations.client.OperationsClient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.hibernate.annotations.NaturalId;
import org.javers.core.metamodel.annotation.PropertyName;
import org.javers.core.metamodel.annotation.TypeName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import javax.inject.Inject;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

@SpringBootTest(
  properties = {"dev-user.enabled: true", "keycloak.enabled: false"},
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
public class DinaRepoBulkOperationIT extends BaseRestAssuredTest {

  @Inject
  private CrnkBoot boot;
  @Inject
  private DinaRepository<ProjectDTO, Project> projectRepo;
  @Inject
  private DinaRepository<TaskDTO, Task> taskRepo;

  private OperationsClient operationsClient;

  public DinaRepoBulkOperationIT() {
    super("");
  }

  @BeforeEach
  void setUp() {
    String url = "http://localhost:" + super.testPort + "/" + super.basePath;
    CrnkClient client = new CrnkClient(url);
    operationsClient = new OperationsClient(client);
    client.setHttpAdapter(new InMemoryHttpAdapter(boot, url));
  }

  @AfterEach
  void tearDown() {
    //Clean up test data
    projectRepo.findAll(createProjectQuerySpec())
      .forEach(projectDTO -> projectRepo.delete(projectDTO.getUuid()));
    taskRepo.findAll(new QuerySpec(TaskDTO.class)).forEach(task -> taskRepo.delete(task.getUuid()));
  }

  @Test
  void bulkPost_ResourcesCreatedWithRelationships() {
    ProjectDTO project1 = createProjectDTO();
    project1.setTask(createTaskDTO());
    ProjectDTO project2 = createProjectDTO();
    project2.setTask(createTaskDTO());

    OperationsCall call = operationsClient.createCall();
    call.add(HttpMethod.POST, project1.getTask());
    call.add(HttpMethod.POST, project2.getTask());
    call.add(HttpMethod.POST, project1);
    call.add(HttpMethod.POST, project2);
    call.execute();

    Assertions.assertEquals(2, projectRepo.findAll(createProjectQuerySpec()).size());
    Assertions.assertEquals(2, taskRepo.findAll(new QuerySpec(TaskDTO.class)).size());

    assertProject(project1, projectRepo.findOne(project1.getUuid(), createProjectQuerySpec()));
    assertProject(project2, projectRepo.findOne(project2.getUuid(), createProjectQuerySpec()));
  }

  @Test
  void bulkDelete_ResourcesDeleted() {
    ProjectDTO project1 = createProjectDTO();
    ProjectDTO project2 = createProjectDTO();

    OperationsCall call = operationsClient.createCall();
    call.add(HttpMethod.POST, project1);
    call.add(HttpMethod.POST, project2);
    call.execute();

    Assertions.assertEquals(2, projectRepo.findAll(createProjectQuerySpec()).size());

    call = operationsClient.createCall();
    call.add(HttpMethod.DELETE, project1);
    call.add(HttpMethod.DELETE, project2);
    call.execute();

    Assertions.assertEquals(0, projectRepo.findAll(createProjectQuerySpec()).size());
  }

  @Test
  void bulkUpdate_ResourcesUpdatedWithRelationship() {
    ProjectDTO project1 = createProjectDTO();
    ProjectDTO project2 = createProjectDTO();

    OperationsCall call = operationsClient.createCall();
    call.add(HttpMethod.POST, project1);
    call.add(HttpMethod.POST, project2);
    call.execute();

    project1.setName(RandomStringUtils.randomAlphabetic(5));
    project1.setTask(createTaskDTO());
    project2.setName(RandomStringUtils.randomAlphabetic(5));
    project2.setTask(createTaskDTO());

    call = operationsClient.createCall();
    call.add(HttpMethod.POST, project1.getTask());
    call.add(HttpMethod.POST, project2.getTask());
    call.add(HttpMethod.PATCH, project1);
    call.add(HttpMethod.PATCH, project2);
    call.execute();

    assertProject(project1, projectRepo.findOne(project1.getUuid(), createProjectQuerySpec()));
    assertProject(project2, projectRepo.findOne(project2.getUuid(), createProjectQuerySpec()));
  }

  @Test
  void partialUpdate_EmptyPatch_NothingChanges() {
    ProjectDTO project1 = createProjectDTO();
    project1.setTask(createTaskDTO());

    OperationsCall call = operationsClient.createCall();
    call.add(HttpMethod.POST, project1.getTask());
    call.add(HttpMethod.POST, project1);
    call.execute();

    //Assert correct state
    assertProject(project1, projectRepo.findOne(project1.getUuid(), createProjectQuerySpec()));

    //Send empty patch
    super.sendPatch(
      ProjectDTO.RESOURCE_TYPE,
      project1.getUuid().toString(),
      ImmutableMap.of(
        "data",
        ImmutableMap.of(
          "type", ProjectDTO.RESOURCE_TYPE,
          "attributes", Collections.emptyMap()
        )));

    //Assert Nothing Changes
    assertProject(project1, projectRepo.findOne(project1.getUuid(), createProjectQuerySpec()));
  }

  private void assertProject(ProjectDTO expected, ProjectDTO result) {
    Assertions.assertEquals(expected.getUuid(), result.getUuid());
    Assertions.assertEquals(expected.getName(), result.getName());
    Assertions.assertEquals(expected.getAcMetaDataCreator(), result.getAcMetaDataCreator());
    if (expected.getTask() != null) {
      Assertions.assertNotNull(result.getTask());
      Assertions.assertEquals(expected.getTask().getUuid(), result.getTask().getUuid());
      Assertions.assertEquals(expected.getTask().getPowerLevel(), result.getTask().getPowerLevel());
    } else {
      Assertions.assertNull(result.getTask());
    }
  }

  private static ProjectDTO createProjectDTO() {
    return ProjectDTO.builder()
      .name(RandomStringUtils.randomAlphabetic(5))
      .acMetaDataCreator(UUID.randomUUID())
      .uuid(UUID.randomUUID())
      .build();
  }

  private static TaskDTO createTaskDTO() {
    return TaskDTO.builder().uuid(UUID.randomUUID()).powerLevel(RandomUtils.nextInt()).build();
  }

  private static QuerySpec createProjectQuerySpec() {
    QuerySpec querySpec = new QuerySpec(ProjectDTO.class);
    querySpec.includeRelation(PathSpec.of("task"));
    return querySpec;
  }

  @TestConfiguration
  @EntityScan(basePackageClasses = DinaRepoBulkOperationIT.class)
  static class DinaRepoBulkOperationITConfig {
    @Bean
    public DinaRepository<ProjectDTO, Project> projectRepo(
      BaseDAO baseDAO,
      DinaFilterResolver filterResolver
    ) {
      return new DinaRepository<>(
        new DinaService<>(baseDAO),
        Optional.empty(),
        Optional.empty(),
        new DinaMapper<>(ProjectDTO.class),
        ProjectDTO.class,
        Project.class,
        filterResolver
      );
    }

    @Bean
    public DinaRepository<TaskDTO, Task> taskRepo(
      BaseDAO baseDAO,
      DinaFilterResolver filterResolver
    ) {
      return new DinaRepository<>(
        new DinaService<>(baseDAO),
        Optional.empty(),
        Optional.empty(),
        new DinaMapper<>(TaskDTO.class),
        TaskDTO.class,
        Task.class,
        filterResolver
      );
    }
  }

  @Data
  @Entity
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static final class Project implements DinaEntity {
    @Id
    @GeneratedValue
    private Integer id;
    @NaturalId
    private UUID uuid;
    private String name;
    private OffsetDateTime createdOn;
    private String createdBy;
    @OneToOne
    @JoinColumn(name = "task_id")
    private Task task;
    private UUID acMetaDataCreator;
  }

  @Data
  @JsonApiResource(type = ProjectDTO.RESOURCE_TYPE)
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @RelatedEntity(Project.class)
  @TypeName(ProjectDTO.RESOURCE_TYPE)
  public static final class ProjectDTO {
    public static final String RESOURCE_TYPE = "Project";
    @JsonApiId
    @org.javers.core.metamodel.annotation.Id
    @PropertyName("id")
    private UUID uuid;
    private String name;
    @JsonApiRelation
    private TaskDTO task;
    @JsonApiExternalRelation(type = "Person")
    private UUID acMetaDataCreator;
  }

  @Data
  @Entity
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static final class Task implements DinaEntity {
    @Id
    @GeneratedValue
    private Integer id;
    @NaturalId
    private UUID uuid;
    private int powerLevel;
    private OffsetDateTime createdOn;
    private String createdBy;
  }

  @Data
  @JsonApiResource(type = TaskDTO.RESOURCE_TYPE)
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @RelatedEntity(Task.class)
  @TypeName(TaskDTO.RESOURCE_TYPE)
  public static final class TaskDTO {
    public static final String RESOURCE_TYPE = "Task";
    @JsonApiId
    @org.javers.core.metamodel.annotation.Id
    @PropertyName("id")
    private UUID uuid;
    private int powerLevel;
  }

}
