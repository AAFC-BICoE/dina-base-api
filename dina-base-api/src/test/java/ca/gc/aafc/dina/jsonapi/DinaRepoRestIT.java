package ca.gc.aafc.dina.jsonapi;

import ca.gc.aafc.dina.ExternalResourceProviderImplementation;
import ca.gc.aafc.dina.dto.ExternalRelationDto;
import ca.gc.aafc.dina.dto.ProjectDTO;
import ca.gc.aafc.dina.dto.TaskDTO;
import ca.gc.aafc.dina.entity.ComplexObject;
import ca.gc.aafc.dina.entity.Project;
import ca.gc.aafc.dina.entity.Task;
import ca.gc.aafc.dina.jpa.BaseDAO;
import ca.gc.aafc.dina.mapper.DinaMapper;
import ca.gc.aafc.dina.repository.DinaRepository;
import ca.gc.aafc.dina.repository.external.ExternalResourceProvider;
import ca.gc.aafc.dina.security.auth.AllowAllAuthorizationService;
import ca.gc.aafc.dina.service.DefaultDinaService;
import ca.gc.aafc.dina.testsupport.BaseRestAssuredTest;
import ca.gc.aafc.dina.testsupport.PostgresTestContainerInitializer;
import ca.gc.aafc.dina.testsupport.jsonapi.JsonAPIOperationBuilder;
import ca.gc.aafc.dina.testsupport.jsonapi.JsonAPIRelationship;
import ca.gc.aafc.dina.testsupport.jsonapi.JsonAPITestHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.crnk.core.queryspec.PathSpec;
import io.crnk.core.queryspec.QuerySpec;
import io.restassured.http.Header;
import io.restassured.response.ValidatableResponse;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.validation.SmartValidator;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@TestConfiguration
@SpringBootTest(
  properties = {"dev-user.enabled: true", "keycloak.enabled: false"},
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(DinaRepoRestIT.DinaRepoBulkOperationITConfig.class)
@ContextConfiguration(initializers = { PostgresTestContainerInitializer.class })
public class DinaRepoRestIT extends BaseRestAssuredTest {

  private static final Header CRNK_HEADER = new Header("crnk-compact", "true");

  @Inject
  private DinaRepository<ProjectDTO, Project> projectRepo;
  @Inject
  private DinaRepository<TaskDTO, Task> taskRepo;

  public DinaRepoRestIT() {
    super("");
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
    TaskDTO task = sendTask();
    ProjectDTO expected = sendProjectByOperations(task);
    ProjectDTO result = projectRepo.findOne(expected.getUuid(), createProjectQuerySpec());
    assertProject(expected, result);
  }

  @Test
  void bulkDelete_ResourcesDeleted() {
    TaskDTO task = sendTask();
    ProjectDTO expected = sendProjectByOperations(task);
    assertEquals(1, projectRepo.findAll(createProjectQuerySpec()).size());

    Map<String, Object> map = JsonAPITestHelper.toJsonAPIMap(
      ProjectDTO.RESOURCE_TYPE, Collections.emptyMap(), null, expected.getUuid().toString());
    String path = ProjectDTO.RESOURCE_TYPE + "/" + expected.getUuid().toString();
    List<Map<String, Object>> operationMap = JsonAPIOperationBuilder.newBuilder()
      .addOperation(HttpMethod.DELETE, path, map).buildOperation();

    sendOperation(operationMap);
    assertEquals(0, projectRepo.findAll(createProjectQuerySpec()).size());
  }

  @Test
  void bulkUpdate_ResourcesUpdatedWithRelationship() {
    TaskDTO task = sendTask();
    ProjectDTO persisted = sendProjectByOperations(task);
    String expectedName = "new name";
    TaskDTO expectedTask = sendTask();

    List<JsonAPIRelationship> relationship = Collections.singletonList(
      JsonAPIRelationship.of("task", TaskDTO.RESOURCE_TYPE, expectedTask.getUuid().toString()));
    Map<String, Object> map = JsonAPITestHelper.toJsonAPIMap(
      ProjectDTO.RESOURCE_TYPE,
      JsonAPITestHelper.toAttributeMap(ProjectDTO.builder().name(expectedName).build()),
      JsonAPITestHelper.toRelationshipMap(relationship),
      persisted.getUuid().toString());
    String path = ProjectDTO.RESOURCE_TYPE + "/" + persisted.getUuid().toString();
    List<Map<String, Object>> operationMap = JsonAPIOperationBuilder.newBuilder()
      .addOperation(HttpMethod.PATCH, path, map).buildOperation();

    sendOperation(operationMap);
    ProjectDTO result = projectRepo.findOne(persisted.getUuid(), createProjectQuerySpec());
    assertEquals(expectedName, result.getName());
    assertEquals(expectedTask.getUuid(), result.getTask().getUuid());
    assertEquals(expectedTask.getPowerLevel(), result.getTask().getPowerLevel());
  }

  @Test
  void partialUpdate_EmptyPatch_NothingChanges() {
    TaskDTO task = sendTask();
    ProjectDTO expected = sendProjectByOperations(task);

    //Assert correct state
    assertProject(expected, projectRepo.findOne(expected.getUuid(), createProjectQuerySpec()));

    //Send empty patch
    sendPatch(ProjectDTO.RESOURCE_TYPE, expected.getUuid().toString(),
        JsonAPITestHelper.toJsonAPIMap(ProjectDTO.RESOURCE_TYPE, Collections.emptyMap()));

    //Assert Nothing Changes
    assertProject(expected, projectRepo.findOne(expected.getUuid(), createProjectQuerySpec()));
  }

  @Test
  void partialUpdate_SingleFieldPatch_Only1FieldChanged() {
    TaskDTO task = sendTask();
    ProjectDTO expected = sendProjectByOperations(task);

    //Assert correct state
    assertProject(expected, projectRepo.findOne(expected.getUuid(), createProjectQuerySpec()));
    assertNotNull(expected.getAlias());

    //Patch a single field
    sendPatch(ProjectDTO.RESOURCE_TYPE, expected.getUuid().toString(),
        JsonAPITestHelper.toJsonAPIMap(ProjectDTO.RESOURCE_TYPE, Map.of("name", "changedName" )));

    //Assert that Alias hasn't Changed
    ProjectDTO found = projectRepo.findOne(expected.getUuid(), createProjectQuerySpec());
    assertEquals(expected.getAlias(), found.getAlias());
    assertNotEquals(expected.getName(), found.getName());
  }

  @Test
  void metaInfo_find_metaInfoContainsExternalRelation() {
    validateMetaOfResponse(super.sendGet(
      ProjectDTO.RESOURCE_TYPE,
      sendProjectByOperations(sendTask()).getUuid().toString()));
    validateMetaOfResponse(super.sendGet(ProjectDTO.RESOURCE_TYPE, ""));
  }

  private static void validateMetaOfResponse(ValidatableResponse response) {
    response.body("meta.external[0].type", Matchers.equalTo("agent"));
    response.body("meta.external[0].href", Matchers.equalTo(
      ExternalResourceProviderImplementation.typeToReferenceMap.get("agent")));
    response.body("meta.external[1].type", Matchers.equalTo("author"));
    response.body("meta.external[1].href", Matchers.equalTo(
      ExternalResourceProviderImplementation.typeToReferenceMap.get("author")));
  }

  @Test
  void metaInfo_WhenNoExternalTypes_ExcludedFromMetaInfo() {
    ValidatableResponse validatableResponse = super.sendGet(
      TaskDTO.RESOURCE_TYPE,
      sendTask().getUuid().toString());
    validatableResponse.body("meta.external", Matchers.nullValue());
  }

  @Test
  void findAll_withExternalRelations_IncludedInResponse() {
    TaskDTO task = sendTask();
    ProjectDTO expected = sendProjectByOperations(task);

    ValidatableResponse response = given()
      .header(CRNK_HEADER).port(testPort).basePath(basePath)
      .queryParams(Map.of("include", "acMetaDataCreator,originalAuthor"))
      .get(ProjectDTO.RESOURCE_TYPE + "/" + expected.getUuid().toString())
      .then();

    response.body(
      "data.relationships.acMetaDataCreator.data.id",
      Matchers.equalTo(expected.getAcMetaDataCreator().getId()));
    response.body(
      "data.relationships.originalAuthor.data.id",
      Matchers.equalTo(expected.getOriginalAuthor().getId()));
    response.body("data.relationships.acMetaDataCreator.data.type", Matchers.equalTo("agent"));
    response.body("data.relationships.originalAuthor.data.type", Matchers.equalTo("author"));
  }

  @Test
  // Test fails for CRNK 3.3.x and 3.4.x https://github.com/crnk-project/crnk-framework/issues/790
  void jsonIncludeNonEmpty_WhenCollectionPresent_SerializesCollection() {
    ValidatableResponse findOne = super.sendGet(
      ProjectDTO.RESOURCE_TYPE,
      sendProjectByOperations(sendTask()).getUuid().toString());
    ValidatableResponse findAll = super.sendGet(ProjectDTO.RESOURCE_TYPE, "");
    findOne.body("data.attributes.nameTranslations", Matchers.notNullValue());
    findAll.body("data[0].attributes.nameTranslations", Matchers.notNullValue());
  }

  private TaskDTO sendTask() {
    TaskDTO task = TaskDTO.builder().powerLevel(RandomUtils.nextInt()).build();
    UUID uuid = UUID.randomUUID();
    String id = sendPost(TaskDTO.RESOURCE_TYPE, JsonAPITestHelper.toJsonAPIMap(
      TaskDTO.RESOURCE_TYPE, JsonAPITestHelper.toAttributeMap(task), null, uuid.toString()))
      .extract().body().jsonPath().getString("data.id");
    task.setUuid(UUID.fromString(id));
    return task;
  }

  private ProjectDTO sendProjectByOperations(TaskDTO task) {
    ProjectDTO project = ProjectDTO.builder()
      .nameTranslations(Collections.singletonList(ComplexObject.builder().name("complex").build()))
      .alias(RandomStringUtils.randomAlphabetic(5))
      .name("DinaRepoRestIT_" + RandomStringUtils.randomAlphabetic(5)).build();
    String agentID = UUID.randomUUID().toString();
    String authorID = UUID.randomUUID().toString();

    Map<String, Object> map = mapProject(project, agentID, authorID, task.getUuid().toString());
    List<Map<String, Object>> operationMap = JsonAPIOperationBuilder.newBuilder()
      .addOperation(HttpMethod.POST, ProjectDTO.RESOURCE_TYPE, map)
      .buildOperation();

    ValidatableResponse operationResponse = sendOperation(operationMap);
    String resultID = operationResponse.extract().body().jsonPath().getString("[0].data.id");
    project.setUuid(UUID.fromString(resultID));
    project.setOriginalAuthor(ExternalRelationDto.builder().id(authorID).build());
    project.setAcMetaDataCreator(ExternalRelationDto.builder().id(agentID).build());
    project.setTask(task);
    return project;
  }

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
    assertEquals(expected.getName(), result.getName());
    assertExternalType(expected.getAcMetaDataCreator(), result.getAcMetaDataCreator());
    assertExternalType(expected.getOriginalAuthor(), result.getOriginalAuthor());
    if (expected.getTask() != null) {
      assertNotNull(result.getTask());
      assertEquals(expected.getTask().getUuid(), result.getTask().getUuid());
      assertEquals(expected.getTask().getPowerLevel(), result.getTask().getPowerLevel());
    } else {
      assertNull(result.getTask());
    }
  }

  private static void assertExternalType(ExternalRelationDto expected, ExternalRelationDto result) {
    if (expected == null) {
      assertNull(result);
    } else {
      assertNotNull(result);
      assertEquals(expected.getId(), result.getId());
    }
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
      ExternalResourceProvider externalResourceProvider,
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
        new BuildProperties(new Properties()), objMapper
      );
    }

    @Bean
    public DinaRepository<TaskDTO, Task> taskRepo(
      ExternalResourceProvider externalResourceProvider,
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
        new BuildProperties(new Properties()), objMapper
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
