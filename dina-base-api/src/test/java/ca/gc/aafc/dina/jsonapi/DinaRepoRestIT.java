package ca.gc.aafc.dina.jsonapi;

import ca.gc.aafc.dina.ExternalResourceProviderImplementation;
import ca.gc.aafc.dina.dto.ExternalRelationDto;
import ca.gc.aafc.dina.dto.ProjectDTO;
import ca.gc.aafc.dina.dto.TaskDTO;
import ca.gc.aafc.dina.entity.Project;
import ca.gc.aafc.dina.entity.Task;
import ca.gc.aafc.dina.filter.DinaFilterResolver;
import ca.gc.aafc.dina.jpa.BaseDAO;
import ca.gc.aafc.dina.mapper.DinaMapper;
import ca.gc.aafc.dina.repository.DinaRepository;
import ca.gc.aafc.dina.repository.meta.ExternalResourceProvider;
import ca.gc.aafc.dina.service.DinaService;
import ca.gc.aafc.dina.testsupport.BaseRestAssuredTest;
import ca.gc.aafc.dina.testsupport.jsonapi.JsonAPIOperationBuilder;
import ca.gc.aafc.dina.testsupport.jsonapi.JsonAPIRelationship;
import ca.gc.aafc.dina.testsupport.jsonapi.JsonAPITestHelper;
import com.google.common.collect.ImmutableMap;
import io.crnk.client.CrnkClient;
import io.crnk.client.http.inmemory.InMemoryHttpAdapter;
import io.crnk.core.boot.CrnkBoot;
import io.crnk.core.engine.http.HttpMethod;
import io.crnk.core.queryspec.PathSpec;
import io.crnk.core.queryspec.QuerySpec;
import io.crnk.operations.client.OperationsCall;
import io.crnk.operations.client.OperationsClient;
import io.restassured.http.Header;
import io.restassured.response.ValidatableResponse;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Matchers;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static io.restassured.RestAssured.given;

@SpringBootTest(
  properties = {"dev-user.enabled: true", "keycloak.enabled: false"},
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(DinaRepoRestIT.DinaRepoBulkOperationITConfig.class)
public class DinaRepoRestIT extends BaseRestAssuredTest {

  private static final Header CRNK_HEADER = new Header("crnk-compact", "true");

  @Inject
  private CrnkBoot boot;
  @Inject
  private DinaRepository<ProjectDTO, Project> projectRepo;
  @Inject
  private DinaRepository<TaskDTO, Task> taskRepo;

  private OperationsClient operationsClient;

  public DinaRepoRestIT() {
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
    TaskDTO task = postTask();
    ProjectDTO project = ProjectDTO.builder().name(RandomStringUtils.randomAlphabetic(5)).build();

    String agentID = UUID.randomUUID().toString();
    String authorID = UUID.randomUUID().toString();

    List<Map<String, Object>> operationMap = JsonAPIOperationBuilder.newBuilder()
      .addOperation(
        HttpMethod.POST,
        ProjectDTO.RESOURCE_TYPE,
        mapProject(project, agentID, authorID, task.getUuid().toString()))
      .buildOperation();

    ValidatableResponse operationResponse = sendOperation(operationMap);
    String resultID = operationResponse.extract().body().jsonPath().getString("[0].data.id");
    ProjectDTO result = projectRepo.findOne(UUID.fromString(resultID), createProjectQuerySpec());

    Assertions.assertEquals(project.getName(), result.getName());
    Assertions.assertEquals(agentID, result.getAcMetaDataCreator().getId());
    Assertions.assertEquals(authorID, result.getOriginalAuthor().getId());

    Assertions.assertEquals(task.getUuid(), result.getTask().getUuid());
    Assertions.assertEquals(task.getPowerLevel(), result.getTask().getPowerLevel());
  }

  @NotNull
  private TaskDTO postTask() {
    TaskDTO task = TaskDTO.builder().powerLevel(RandomUtils.nextInt()).build();
    String taskid = sendPost(
      TaskDTO.RESOURCE_TYPE,
      JsonAPITestHelper.toJsonAPIMap(TaskDTO.RESOURCE_TYPE, JsonAPITestHelper.toAttributeMap(task),
        null,
        UUID.randomUUID().toString())).extract().body().jsonPath().getString("data.id");
    task.setUuid(UUID.fromString(taskid));
    return task;
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

  @Test
  void metaInfo_findOne_metaInfoContainsExternalRelation() {
    ProjectDTO project1 = createProjectDTO();
    project1.setTask(createTaskDTO());

    OperationsCall call = operationsClient.createCall();
    call.add(HttpMethod.POST, project1.getTask());
    call.add(HttpMethod.POST, project1);
    call.execute();

    ValidatableResponse validatableResponse = super.sendGet(
      ProjectDTO.RESOURCE_TYPE,
      project1.getUuid().toString());

    ExternalResourceProviderImplementation.typeToReferenceMap.forEach((key, value) ->
      validatableResponse.body("meta.externalTypes." + key, Matchers.equalTo(value)));
  }

  @Test
  void metaInfo_findAll_metaInfoContainsExternalRelation() {
    ProjectDTO project1 = createProjectDTO();
    ProjectDTO project2 = createProjectDTO();

    OperationsCall call = operationsClient.createCall();
    call.add(HttpMethod.POST, project1);
    call.add(HttpMethod.POST, project2);
    call.execute();

    ValidatableResponse validatableResponse = super.sendGet(ProjectDTO.RESOURCE_TYPE, "");
    ExternalResourceProviderImplementation.typeToReferenceMap.forEach((key, value) ->
      validatableResponse.body("meta.externalTypes." + key, Matchers.equalTo(value)));
  }

  @Test
  void metaInfo_WhenNoExternalTypes_ExcludedFromMetaInfo() {
    TaskDTO taskDTO = createTaskDTO();

    OperationsCall call = operationsClient.createCall();
    call.add(HttpMethod.POST, taskDTO);
    call.execute();

    ValidatableResponse validatableResponse = super.sendGet(
      TaskDTO.RESOURCE_TYPE,
      taskDTO.getUuid().toString());
    validatableResponse.body("meta.externalTypes", Matchers.nullValue());
  }

  @Test
  void findAll_withExternalRelations_IncludedInResponse() {
    ProjectDTO project1 = createProjectDTO();
    project1.setUuid(null);
    project1.setAcMetaDataCreator(null);
    project1.setOriginalAuthor(null);

    String agentID = UUID.randomUUID().toString();
    String authorID = UUID.randomUUID().toString();
    String taskID = UUID.randomUUID().toString();

    String id = sendPost(ProjectDTO.RESOURCE_TYPE, mapProject(project1, agentID, authorID, taskID))
      .extract().body().jsonPath().getString("data.id");

    ValidatableResponse response = given()
      .header(CRNK_HEADER).port(testPort).basePath(basePath)
      .queryParams(ImmutableMap.of("include", "acMetaDataCreator,originalAuthor"))
      .get(StringUtils.appendIfMissing(ProjectDTO.RESOURCE_TYPE, "/") + id)
      .then();

    response.body(
      "data.relationships.acMetaDataCreator.data.id",
      Matchers.equalTo(agentID));
    response.body(
      "data.relationships.originalAuthor.data.id",
      Matchers.equalTo(authorID));
    response.log().all(true);//TODO remove me
  }

  @NotNull
  private Map<String, Object> mapProject(
    ProjectDTO project,
    String agentID,
    String authorID,
    String taskID
  ) {
    return JsonAPITestHelper.toJsonAPIMap(
      ProjectDTO.RESOURCE_TYPE,
      JsonAPITestHelper.toAttributeMap(project),
      JsonAPITestHelper.toRelationshipMap(
        Arrays.asList(
          JsonAPIRelationship.of("acMetaDataCreator", "agent", agentID),
          JsonAPIRelationship.of("originalAuthor", "author", authorID),
          JsonAPIRelationship.of("task", TaskDTO.RESOURCE_TYPE, taskID))),
      UUID.randomUUID().toString()
    );
  }

  private void assertProject(ProjectDTO expected, ProjectDTO result) {
    Assertions.assertEquals(expected.getName(), result.getName());
    assertExternalType(expected.getAcMetaDataCreator(), result.getAcMetaDataCreator());
    assertExternalType(expected.getOriginalAuthor(), result.getOriginalAuthor());
    if (expected.getTask() != null) {
      Assertions.assertNotNull(result.getTask());
      Assertions.assertEquals(expected.getTask().getUuid(), result.getTask().getUuid());
      Assertions.assertEquals(expected.getTask().getPowerLevel(), result.getTask().getPowerLevel());
    } else {
      Assertions.assertNull(result.getTask());
    }
  }

  private static void assertExternalType(ExternalRelationDto expected, ExternalRelationDto result) {
    if (expected == null) {
      Assertions.assertNull(result);
    } else {
      Assertions.assertNotNull(result);
      Assertions.assertEquals(expected.getId(), result.getId());
    }
  }

  private static ProjectDTO createProjectDTO() {
    return ProjectDTO.builder()
      .name(RandomStringUtils.randomAlphabetic(5))
      .build();
  }

  private static TaskDTO createTaskDTO() {
    return TaskDTO.builder().powerLevel(RandomUtils.nextInt()).build();
  }

  private static QuerySpec createProjectQuerySpec() {
    QuerySpec querySpec = new QuerySpec(ProjectDTO.class);
    querySpec.includeRelation(PathSpec.of("task"));
    querySpec.includeRelation(PathSpec.of("acMetaDataCreator"));
    querySpec.includeRelation(PathSpec.of("originalAuthor"));
    return querySpec;
  }

  @TestConfiguration
  @EntityScan(basePackageClasses = DinaRepoRestIT.class)
  @Import(ExternalResourceProviderImplementation.class)
  static class DinaRepoBulkOperationITConfig {
    @Bean
    public DinaRepository<ProjectDTO, Project> projectRepo(
      BaseDAO baseDAO,
      DinaFilterResolver filterResolver,
      ExternalResourceProvider externalResourceProvider
    ) {
      return new DinaRepository<>(
        new DinaService<>(baseDAO),
        Optional.empty(),
        Optional.empty(),
        new DinaMapper<>(ProjectDTO.class),
        ProjectDTO.class,
        Project.class,
        filterResolver,
        externalResourceProvider
      );
    }

    @Bean
    public DinaRepository<TaskDTO, Task> taskRepo(
      BaseDAO baseDAO,
      DinaFilterResolver filterResolver,
      ExternalResourceProvider externalResourceProvider
    ) {
      return new DinaRepository<>(
        new DinaService<>(baseDAO),
        Optional.empty(),
        Optional.empty(),
        new DinaMapper<>(TaskDTO.class),
        TaskDTO.class,
        Task.class,
        filterResolver,
        externalResourceProvider
      );
    }
  }

}
