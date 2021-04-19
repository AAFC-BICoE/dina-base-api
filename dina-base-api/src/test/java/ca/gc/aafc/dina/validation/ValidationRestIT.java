package ca.gc.aafc.dina.validation;

import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;

import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.dto.DepartmentDto;
import ca.gc.aafc.dina.dto.ValidationDto;
import ca.gc.aafc.dina.testsupport.jsonapi.JsonAPITestHelper;
import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;

@SpringBootTest(classes = TestDinaBaseApp.class,
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  properties = {"dev-user.enabled: true", "keycloak.enabled: false"})
public class ValidationRestIT {

  @LocalServerPort
  protected int testPort;
  
  @Test
  void validate_Post() {
    newRequest()
      .body(newValidationDto(newLongNameDto()))
      .post("/validation")
      .then()
      .body("errors[0].status", Matchers.equalToIgnoringCase("200"));
  }

  private RequestSpecification newRequest() {
    return RestAssured.given().port(this.testPort).contentType("application/vnd.api+json");
  }

  private Map<String, Object> newValidationDto(Map<String, Object> departmentDto) {
    ValidationDto dto = ValidationDto.builder().type("department").data(departmentDto).build();
    return JsonAPITestHelper.toJsonAPIMap("validation", JsonAPITestHelper.toAttributeMap(dto));
  }
  
  private Map<String, Object> newDto() {
    DepartmentDto dto = DepartmentDto.builder().name("dfadf").location(null).build();
    return JsonAPITestHelper.toJsonAPIMap("department", JsonAPITestHelper.toAttributeMap(dto));
  }

  private Map<String, Object> newLongNameDto() {
    DepartmentDto dto = DepartmentDto.builder().name("01234567890123456789012345678901234567890123456789a").location("Montreal").build();
    return JsonAPITestHelper.toJsonAPIMap("department", JsonAPITestHelper.toAttributeMap(dto));
  }

}
