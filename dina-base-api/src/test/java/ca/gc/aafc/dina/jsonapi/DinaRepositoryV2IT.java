package ca.gc.aafc.dina.jsonapi;

import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.gc.aafc.dina.dto.ProjectDTO;
import ca.gc.aafc.dina.dto.TaskDTO;
import ca.gc.aafc.dina.entity.Project;
import ca.gc.aafc.dina.entity.Task;
import ca.gc.aafc.dina.exception.ResourceNotFoundException;
import ca.gc.aafc.dina.mapper.ProjectDtoMapper;
import ca.gc.aafc.dina.mapper.TaskDtoMapper;
import ca.gc.aafc.dina.repository.DinaRepositoryV2;
import ca.gc.aafc.dina.security.auth.AllowAllAuthorizationService;
import ca.gc.aafc.dina.service.DinaService;
import ca.gc.aafc.dina.testsupport.BaseRestAssuredTest;
import ca.gc.aafc.dina.testsupport.PostgresTestContainerInitializer;
import ca.gc.aafc.dina.testsupport.jsonapi.JsonAPIRelationship;
import ca.gc.aafc.dina.testsupport.jsonapi.JsonAPITestHelper;

import static com.toedter.spring.hateoas.jsonapi.MediaTypes.JSON_API_VALUE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.transaction.Transactional;
import lombok.Getter;

@SpringBootTest(
  properties = {"dev-user.enabled: true", "keycloak.enabled: false"},
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = { PostgresTestContainerInitializer.class })
@Import({DinaRepositoryV2IT.TestDynaBeanRepo.class, DinaRepositoryV2IT.RepoV2TestConfig.class})
public class DinaRepositoryV2IT extends BaseRestAssuredTest {

  private static final String PATH = "repo2";

  protected DinaRepositoryV2IT() {
    super("");
  }

  @Test
  public void onPatchToOneRelationship() {
    // Create a project
    ProjectDTO project = ProjectDTO.builder().build();
    UUID projectUuid = UUID.randomUUID();
    sendPost(PATH + "/" + ProjectDTO.RESOURCE_TYPE, JsonAPITestHelper.toJsonAPIMap(
      ProjectDTO.RESOURCE_TYPE, JsonAPITestHelper.toAttributeMap(project), null, projectUuid.toString()));

    // Create a task
    TaskDTO task = TaskDTO.builder().power(RandomUtils.nextInt()).build();
    UUID taskUuid = UUID.randomUUID();
    sendPost(PATH + "/" + TaskDTO.RESOURCE_TYPE, JsonAPITestHelper.toJsonAPIMap(
      TaskDTO.RESOURCE_TYPE, JsonAPITestHelper.toAttributeMap(task), null, taskUuid.toString()));

    // Patch the project to set the task
    int returnCode = sendPatch(PATH + "/" + ProjectDTO.RESOURCE_TYPE , projectUuid.toString(), JsonAPITestHelper.toJsonAPIMap(
      ProjectDTO.RESOURCE_TYPE, JsonAPITestHelper.toAttributeMap(project),
      JsonAPITestHelper.toRelationshipMap(JsonAPIRelationship.of("task", TaskDTO.RESOURCE_TYPE, taskUuid.toString()))
      , projectUuid.toString()))
      .extract().response().getStatusCode();
    assertEquals(200, returnCode);
  }

  @Test
  public void onPatchToManyRelationship() {
    // Create a project
    ProjectDTO project = ProjectDTO.builder().build();
    UUID projectUuid = UUID.randomUUID();
    sendPost(PATH + "/" + ProjectDTO.RESOURCE_TYPE, JsonAPITestHelper.toJsonAPIMap(
      ProjectDTO.RESOURCE_TYPE, JsonAPITestHelper.toAttributeMap(project), null, projectUuid.toString()));

    // Create 2 tasks
    TaskDTO task = TaskDTO.builder().power(RandomUtils.nextInt()).build();
    UUID taskUuid1 = UUID.randomUUID();
    sendPost(PATH + "/" + TaskDTO.RESOURCE_TYPE, JsonAPITestHelper.toJsonAPIMap(
      TaskDTO.RESOURCE_TYPE, JsonAPITestHelper.toAttributeMap(task), null, taskUuid1.toString()));

    TaskDTO task2 = TaskDTO.builder().power(RandomUtils.nextInt()).build();
    UUID taskUuid2 = UUID.randomUUID();
    sendPost(PATH + "/" + TaskDTO.RESOURCE_TYPE, JsonAPITestHelper.toJsonAPIMap(
      TaskDTO.RESOURCE_TYPE, JsonAPITestHelper.toAttributeMap(task2), null, taskUuid2.toString()));

    // Patch the project to set the task
    int returnCode = sendPatch(PATH + "/" + ProjectDTO.RESOURCE_TYPE, projectUuid.toString(),
      JsonAPITestHelper.toJsonAPIMap(
        ProjectDTO.RESOURCE_TYPE, JsonAPITestHelper.toAttributeMap(project),
        JsonAPITestHelper.toRelationshipMapByName(List.of(
          JsonAPIRelationship.of("taskHistory", TaskDTO.RESOURCE_TYPE, taskUuid1.toString()),
          JsonAPIRelationship.of("taskHistory", TaskDTO.RESOURCE_TYPE, taskUuid2.toString())))
        , projectUuid.toString()))
      .extract().response().getStatusCode();

    assertEquals(200, returnCode);
  }

  /**
   * TestDynaBeanRepo is a REST controller that handles HTTP requests for creating and updating
   * Project and Task resources using JSON:API specification.
   */
  @RestController
  @RequestMapping(produces = JSON_API_VALUE)
  static class TestDynaBeanRepo {

    @Getter
    private Integer level;

    @Autowired
    private DinaRepositoryV2<ProjectDTO, Project> projectRepo;

    @Autowired
    private DinaRepositoryV2<TaskDTO, Task> taskRepo;

    @PostMapping(PATH + "/" + TaskDTO.RESOURCE_TYPE)
    @Transactional
    public ResponseEntity<RepresentationModel<?>> handlePostTask(@RequestBody EntityModel<TaskDTO> taskDTO) {
      taskRepo.create(taskDTO.getContent());
      return ResponseEntity.created(URI.create("/")).build();
    }

    @PostMapping(PATH + "/" + ProjectDTO.RESOURCE_TYPE)
    @Transactional
    public ResponseEntity<RepresentationModel<?>> handlePostProject(@RequestBody EntityModel<ProjectDTO> projectDTO) {
      projectRepo.create(projectDTO.getContent());
      return ResponseEntity.created(URI.create("/")).build();
    }

    @PatchMapping(PATH + "/" + ProjectDTO.RESOURCE_TYPE + "/{id}")
    @Transactional
    public ResponseEntity<RepresentationModel<?>> handlePatch(@RequestBody JsonApiDocument partialPatchDto,
                                                              @PathVariable String id)
      throws ResourceNotFoundException {
      projectRepo.update(partialPatchDto);
      return ResponseEntity.ok().build();
    }
  }

  @TestConfiguration
  static class RepoV2TestConfig {

    @Bean
    public DinaRepositoryV2<TaskDTO, Task> taskRepositoryV2(DinaService<Task> dinaService,
                                                                  BuildProperties buildProperties,
                                                                  ObjectMapper objMapper) {
      return new DinaRepositoryV2<>(dinaService, new AllowAllAuthorizationService(),
        Optional.empty(), TaskDtoMapper.INSTANCE, TaskDTO.class, Task.class,
        buildProperties, objMapper);
    }

    @Bean
    public DinaRepositoryV2<ProjectDTO, Project> projectRepositoryV2(DinaService<Project> dinaService,
                                                                  BuildProperties buildProperties,
                                                                  ObjectMapper objMapper) {
      return new DinaRepositoryV2<>(dinaService, new AllowAllAuthorizationService(),
        Optional.empty(), ProjectDtoMapper.INSTANCE, ProjectDTO.class, Project.class,
        buildProperties, objMapper);
    }
  }
}
