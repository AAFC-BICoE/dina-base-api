package ca.gc.aafc.dina.security;

import ca.gc.aafc.dina.testsupport.BaseRestAssuredTest;
import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static io.restassured.RestAssured.given;

@SpringBootTest(
  properties = {"dev-user.enabled: true", "keycloak.enabled: false", "cors.origins: foo.example"},
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CorsConfigurationTest extends BaseRestAssuredTest {

  protected CorsConfigurationTest() {
    super("");
  }

  @Test
  void AccessControlHeaderReturned() {
    Response response = given()
      .header(CRNK_HEADER)
      .header("host", "bar.other")
      .header("origin", "foo.example")
      .port(testPort)
      .basePath(basePath)
      .get("/person");

    response.then().log().all(true);
    Assertions.assertEquals("foo.example", response.header("Access-Control-Allow-Origin"));
  }
}