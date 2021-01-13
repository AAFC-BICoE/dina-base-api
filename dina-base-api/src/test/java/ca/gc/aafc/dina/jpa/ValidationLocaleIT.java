package ca.gc.aafc.dina.jpa;

import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.dto.DepartmentDto;
import ca.gc.aafc.dina.testsupport.BaseRestAssuredTest;
import ca.gc.aafc.dina.testsupport.jsonapi.JsonAPITestHelper;
import io.restassured.RestAssured;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

@SpringBootTest(classes = TestDinaBaseApp.class,
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  properties = {"dev-user.enabled: true", "keycloak.enabled: false"})
public class ValidationLocaleIT extends BaseRestAssuredTest {

  protected ValidationLocaleIT() {
    super("/department");
  }

  @Test
  void validate_OnDifferentLocale_DifferentLocaleUsed() {
    Map<String, Object> map = newDto();

    RestAssured.given()
      .port(this.testPort)
      .contentType("application/vnd.api+json")
      .header("Accept-Language", "fr")
      .body(map)
      .post("/department")
      .then()
      .body("errors[0].status", Matchers.equalToIgnoringCase("422"))
      .body("errors[0].detail", Matchers.equalToIgnoringCase("Test french translation"));
  }

  @Test
  void validate_OnDefaultLocale_DefaultLocaleUsed() {
    Map<String, Object> map = newDto();

    RestAssured.given()
      .port(this.testPort)
      .contentType("application/vnd.api+json")
      .body(map)
      .post("/department")
      .then()
      .body("errors[0].status", Matchers.equalToIgnoringCase("422"))
      .body("errors[0].detail", Matchers.equalToIgnoringCase("location cannot be null."));
  }

  private Map<String, Object> newDto() {
    DepartmentDto dto = DepartmentDto.builder().name("dfadf").location(null).build();
    return JsonAPITestHelper.toJsonAPIMap("department", JsonAPITestHelper.toAttributeMap(dto));
  }
}
