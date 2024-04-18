package ca.gc.aafc.dina.jsonapi;

import java.util.UUID;
import javax.inject.Inject;
import lombok.Getter;

import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ca.gc.aafc.dina.dto.JsonApiPartialPatchDto;
import ca.gc.aafc.dina.dto.TaskDTO;
import ca.gc.aafc.dina.testsupport.BaseRestAssuredTest;
import ca.gc.aafc.dina.testsupport.PostgresTestContainerInitializer;
import ca.gc.aafc.dina.testsupport.jsonapi.JsonAPITestHelper;

import static com.toedter.spring.hateoas.jsonapi.MediaTypes.JSON_API_VALUE;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(
  properties = {"dev-user.enabled: true", "keycloak.enabled: false"},
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = { PostgresTestContainerInitializer.class })
@Import(DinaRepositoryV2IT.TestDynaBeanRepo.class)
public class DinaRepositoryV2IT extends BaseRestAssuredTest {

  private static final String PATH = "dynabean";

  @Inject
  TestDynaBeanRepo dynaBeanRepo;

  protected DinaRepositoryV2IT() {
    super("");
  }

  @Test
  public void sendTask() {
    TaskDTO task = TaskDTO.builder().powerLevel(RandomUtils.nextInt()).build();
    UUID uuid = UUID.randomUUID();
    int returnCode = sendPatch(PATH, uuid.toString(), JsonAPITestHelper.toJsonAPIMap(
      TaskDTO.RESOURCE_TYPE, JsonAPITestHelper.toAttributeMap(task), null, uuid.toString()))
      .extract().response().getStatusCode();

    assertEquals(200, returnCode);
    assertEquals(task.getPowerLevel(), dynaBeanRepo.getLevel());
  }

  /**
   * Test controller that will receive a JsonApiPartialPatchDto for a TaskDto.
   */
  @RestController
  @RequestMapping(produces = JSON_API_VALUE)
  static class TestDynaBeanRepo {

    @Getter
    private Integer level;

    @PatchMapping(PATH + "/{id}")
    public ResponseEntity<RepresentationModel<?>> handlePatch(@RequestBody
                                                              EntityModel<JsonApiPartialPatchDto> partialPatchDto,
                                                              @PathVariable String id) {

      JsonApiPartialPatchDto b = partialPatchDto.getContent();
      level = (Integer)b.get("powerLevel");
      return null;
    }
  }


}
