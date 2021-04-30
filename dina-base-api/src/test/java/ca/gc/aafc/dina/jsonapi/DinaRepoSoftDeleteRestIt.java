package ca.gc.aafc.dina.jsonapi;

import ca.gc.aafc.dina.dto.PersonDTO;
import ca.gc.aafc.dina.testsupport.BaseRestAssuredTest;
import ca.gc.aafc.dina.testsupport.jsonapi.JsonAPITestHelper;
import io.restassured.http.Header;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.UUID;

import static io.restassured.RestAssured.given;

@SpringBootTest(
  properties = {"dev-user.enabled: true", "keycloak.enabled: false", "dina.auditing.enabled = true"},
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class DinaRepoSoftDeleteRestIt extends BaseRestAssuredTest {

  private static final Header CRNK_HEADER = new Header("crnk-compact", "true");

  protected DinaRepoSoftDeleteRestIt() {
    super("/" + PersonDTO.TYPE_NAME);
  }

  @Test
  void findOne_DeletedResourcePresentInAuditLogs_ThrowsGoneExceptionWithLinkToAuditLogs() {
    final String id = sendPost(JsonAPITestHelper.toJsonAPIMap(
      PersonDTO.TYPE_NAME,
      createPersonDto())).extract().body().jsonPath().getString("data.id");
    sendGet(id);
    sendDelete(id);

    String auditRepoLink = sendGet("", id, HttpStatus.SC_GONE).extract()
      .body().jsonPath().getString("errors.meta.link[0]");
    // Assert the returned audit repo url is valid
    given().header(CRNK_HEADER).port(testPort).get(auditRepoLink)
      .then().statusCode(200);
  }

  @Test
  void findOne_DeletedResourceNOTPresentInAuditLogs_ThrowsResourceNotFoundException() {
    sendGet("", UUID.randomUUID().toString(), HttpStatus.SC_NOT_FOUND);
  }

  private static PersonDTO createPersonDto() {
    return PersonDTO.builder()
      .nickNames(Arrays.asList("d", "z", "q").toArray(new String[0]))
      .name(RandomStringUtils.random(4)).build();
  }

}
