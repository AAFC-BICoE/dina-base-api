package ca.gc.aafc.dina.service;

import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.dto.DepartmentDto;
import ca.gc.aafc.dina.testsupport.jsonapi.JsonAPITestHelper;
import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;

import java.util.Map;

@SpringBootTest(classes = TestDinaBaseApp.class,
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  properties = {"dev-user.enabled: true", "keycloak.enabled: false"})
public class DinaServiceValidationLocaleIT {

  @LocalServerPort
  protected int testPort;

  @Test
  void validate_OnDifferentLocale_DifferentLocaleUsed() {
    newRequest()
      .header("Accept-Language", "fr")
      .body(newDto())
      .post("/department")
      .then()
      .body("errors[0].status", Matchers.equalToIgnoringCase("422"))
      .body("errors[0].detail", Matchers.endsWith("Test french translation"));
  }

  @Test
  void validate_OnDefaultLocale_DefaultLocaleUsed() {
    newRequest()
      .body(newDto())
      .post("/department")
      .then()
      .body("errors[0].status", Matchers.equalToIgnoringCase("422"))
      .body("errors[0].detail", Matchers.endsWith("location cannot be null."));
  }

  private RequestSpecification newRequest() {
    return RestAssured.given().port(this.testPort).contentType("application/vnd.api+json");
  }

  private Map<String, Object> newDto() {
    DepartmentDto dto = DepartmentDto.builder().name("dfadf").location(null).build();
    return JsonAPITestHelper.toJsonAPIMap("department", JsonAPITestHelper.toAttributeMap(dto));
  }
}