package ca.gc.aafc.dina.jsonapi;

import ca.gc.aafc.dina.ExternalResourceProviderImplementation;
import ca.gc.aafc.dina.dto.ExternalRelationDto;
import ca.gc.aafc.dina.dto.ProjectDTO;
import ca.gc.aafc.dina.dto.TaskDTO;
import ca.gc.aafc.dina.entity.ComplexObject;
import ca.gc.aafc.dina.entity.Project;
import ca.gc.aafc.dina.entity.Task;
import ca.gc.aafc.dina.filter.DinaFilterResolver;
import ca.gc.aafc.dina.jpa.BaseDAO;
import ca.gc.aafc.dina.mapper.DinaMapper;
import ca.gc.aafc.dina.repository.DinaRepository;
import ca.gc.aafc.dina.repository.external.ExternalResourceProvider;
import ca.gc.aafc.dina.service.DinaService;
import ca.gc.aafc.dina.testsupport.BaseRestAssuredTest;
import ca.gc.aafc.dina.testsupport.jsonapi.JsonAPIOperationBuilder;
import ca.gc.aafc.dina.testsupport.jsonapi.JsonAPIRelationship;
import ca.gc.aafc.dina.testsupport.jsonapi.JsonAPITestHelper;
import com.google.common.collect.ImmutableMap;
import io.crnk.core.engine.http.HttpMethod;
import io.crnk.core.queryspec.PathSpec;
import io.crnk.core.queryspec.QuerySpec;
import io.restassured.http.Header;
import io.restassured.response.ValidatableResponse;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.hamcrest.Matchers;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
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
    Assertions.assertEquals(1, projectRepo.findAll(createProjectQuerySpec()).size());

    Map<String, Object> map = JsonAPITestHelper.toJsonAPIMap(
      ProjectDTO.RESOURCE_TYPE, Collections.emptyMap(), null, expected.getUuid().toString());
    String path = ProjectDTO.RESOURCE_TYPE + "/" + expected.getUuid().toString();
    List<Map<String, Object>> operationMap = JsonAPIOperationBuilder.newBuilder()
      .addOperation(HttpMethod.DELETE, path, map).buildOperation();

    sendOperation(operationMap);
    Assertions.assertEquals(0, projectRepo.findAll(createProjectQuerySpec()).size());
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
    Assertions.assertEquals(expectedName, result.getName());
    Assertions.assertEquals(expectedTask.getUuid(), result.getTask().getUuid());
    Assertions.assertEquals(expectedTask.getPowerLevel(), result.getTask().getPowerLevel());
  }

  @Test
  void partialUpdate_EmptyPatch_NothingChanges() {
    TaskDTO task = sendTask();
    ProjectDTO expected = sendProjectByOperations(task);

    //Assert correct state
    assertProject(expected, projectRepo.findOne(expected.getUuid(), createProjectQuerySpec()));

    //Send empty patch
    super.sendPatch(ProjectDTO.RESOURCE_TYPE, expected.getUuid().toString(),
      ImmutableMap.of(
        "data",
        ImmutableMap.of(
          "type", ProjectDTO.RESOURCE_TYPE,
          "attributes", Collections.emptyMap()
        )));

    //Assert Nothing Changes
    assertProject(expected, projectRepo.findOne(expected.getUuid(), createProjectQuerySpec()));
  }

  @Test
  void metaInfo_find_metaInfoContainsExternalRelation() {
    ValidatableResponse findOne = super.sendGet(
      ProjectDTO.RESOURCE_TYPE,
      sendProjectByOperations(sendTask()).getUuid().toString());
    ValidatableResponse findAll = super.sendGet(ProjectDTO.RESOURCE_TYPE, "");
    ExternalResourceProviderImplementation.typeToReferenceMap.forEach((key, value) ->
    {
      findOne.body("meta.externalTypes." + key, Matchers.equalTo(value));
      findAll.body("meta.externalTypes." + key, Matchers.equalTo(value));
    });
  }

  @Test
  void metaInfo_WhenNoExternalTypes_ExcludedFromMetaInfo() {
    ValidatableResponse validatableResponse = super.sendGet(
      TaskDTO.RESOURCE_TYPE,
      sendTask().getUuid().toString());
    validatableResponse.body("meta.externalTypes", Matchers.nullValue());
  }

  @Test
  void findAll_withExternalRelations_IncludedInResponse() {
    TaskDTO task = sendTask();
    ProjectDTO expected = sendProjectByOperations(task);

    ValidatableResponse response = given()
      .header(CRNK_HEADER).port(testPort).basePath(basePath)
      .queryParams(ImmutableMap.of("include", "acMetaDataCreator,originalAuthor"))
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
      .name(RandomStringUtils.randomAlphabetic(5)).build();
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
