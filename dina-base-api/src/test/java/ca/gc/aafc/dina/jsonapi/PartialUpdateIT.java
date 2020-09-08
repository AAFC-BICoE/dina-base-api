package ca.gc.aafc.dina.jsonapi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Map;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import ca.gc.aafc.dina.dto.DepartmentDto;
import ca.gc.aafc.dina.dto.PersonDTO;
import ca.gc.aafc.dina.testsupport.BaseRestAssuredTest;
import ca.gc.aafc.dina.testsupport.jsonapi.JsonAPIRelationship;
import ca.gc.aafc.dina.testsupport.jsonapi.JsonAPITestHelper;
import io.restassured.response.ValidatableResponse;

@SpringBootTest(classes = ca.gc.aafc.dina.TestConfiguration.class ,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = { "dev-user.enabled: true", "keycloak.enabled: false" })
public class PartialUpdateIT extends BaseRestAssuredTest {

  protected PartialUpdateIT() {
    super("");
  }

  /** Ensures that the partial update bug from #20144 is fixed. */
  @Test
  public void partialUpdate_onSuccess_relationshipsNotRemoved() {
    DepartmentDto department = DepartmentDto.builder().name("dept").location("Ottawa").build();

    // Add a Department:
    ValidatableResponse departmentPostResponse = sendPost(
      "department",
      JsonAPITestHelper.toJsonAPIMap(
        "department", JsonAPITestHelper.toAttributeMap(department), null, null),
      201);

    String departmentUUID = departmentPostResponse.extract().body().jsonPath().getString("data.id");

    PersonDTO person = PersonDTO.builder()
        .nickNames(Arrays.asList("d", "z", "q").toArray(new String[0]))
        .name(RandomStringUtils.randomAlphabetic(4)).build();

    // Add a Person that links to the department:
    ValidatableResponse personResponse = sendPost(
      "person",
      JsonAPITestHelper.toJsonAPIMap(
        "person",
        JsonAPITestHelper.toAttributeMap(person),
        JsonAPITestHelper.toRelationshipMap(Arrays.asList(
          JsonAPIRelationship.of("department", "department", departmentUUID))),
        null),
      201);

    personResponse.statusCode(201);

    String personUUID = personResponse.extract().body().jsonPath().getString("data.id");

    // Do the partial update:
    sendPatch(
      "person",
      personUUID,
      JsonAPITestHelper.toJsonAPIMap(
        "person",
        Map.of("nickNames", Arrays.asList("new nickname")),
        null,
        personUUID),
      200);

    ValidatableResponse patchedPersonResponse = sendGet("person/" + personUUID + "?include=department", 200);

    String postUpdatePersonDepartmentUuid = patchedPersonResponse.extract()
      .body().jsonPath().getString("data.relationships.department.data.id");

    // The Person should still be linked to the same Department:
    assertEquals(departmentUUID, postUpdatePersonDepartmentUuid);
  }

}
