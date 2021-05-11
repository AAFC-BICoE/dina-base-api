package ca.gc.aafc.dina.validation;

import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.dto.DepartmentDto;
import ca.gc.aafc.dina.dto.EmployeeDto;
import ca.gc.aafc.dina.dto.ValidationDto;
import ca.gc.aafc.dina.testsupport.jsonapi.JsonAPITestHelper;
import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;

import java.util.Map;

@SpringBootTest(classes = TestDinaBaseApp.class,
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  properties = {"dev-user.enabled: true", "keycloak.enabled: false", "dina.validationEndpoint.enabled: true"})
public class ValidationRestIT {

  @LocalServerPort
  protected int testPort;

  @Test
  void validateLongNameDepartment_ErrorCode422() {
    newRequest()
      .body(newValidationDto(newLongNameDepartmentDto(), "department"))
      .post("/validation")
      .then()
      .body("errors[0].status", Matchers.equalToIgnoringCase("422"))
      .body("errors[0].detail", Matchers.endsWith("size must be between 1 and 50"));
  }

  @Test
  void validateInvalidEmployee_ErrorCode422() {
    newRequest()
      .body(newValidationDto(newEmployeeDto(), "employee"))
      .post("/validation")
      .then()
      .body("errors[0].status", Matchers.equalToIgnoringCase("422"))
      .body("errors[0].detail", Matchers.endsWith("size must be between 1 and 50"));
  }

  @Test
  void validateValidDepartment_Code200() {
    newRequest()
      .body(newValidationDto(newDepartmentDto(), "department"))
      .post("/validation")
      .then()
      .assertThat().statusCode(201);
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "  ", "\t", "\n", "adfadfadfajdlfk", "null"})
  void validate_WhenTypeBlank_ReturnsBadRequest() {
    newRequest()
      .body(newValidationDto(newDepartmentDto(), ""))
      .post("/validation")
      .then()
      .assertThat().statusCode(400);
  }

  @Test
  void validate_WhenDataBlank_ReturnsBadRequest() {
    // data: null
    Map<String, Object> jsonAPIMap = JsonAPITestHelper.toJsonAPIMap(
      "validation",
      JsonAPITestHelper.toAttributeMap(ValidationDto.builder().type("department").data(null).build()));
    newRequest()
      .body(jsonAPIMap)
      .post("/validation")
      .then()
      .assertThat().statusCode(400);
    // data: {}
    jsonAPIMap = JsonAPITestHelper.toJsonAPIMap("validation", JsonAPITestHelper.toAttributeMap(
      Map.of("type", "department", "data", Map.of())));
    newRequest()
      .body(jsonAPIMap)
      .post("/validation")
      .then()
      .assertThat().statusCode(400);
    // data: { attributes:{} }
    jsonAPIMap = JsonAPITestHelper.toJsonAPIMap("validation", JsonAPITestHelper.toAttributeMap(
      Map.of("type", "department", "data", Map.of("attributes", Map.of()))));
    newRequest()
      .body(jsonAPIMap)
      .post("/validation")
      .then().log().all(true)
      .assertThat().statusCode(400);
  }

  private RequestSpecification newRequest() {
    return RestAssured.given().port(this.testPort).contentType("application/vnd.api+json");
  }

  private Map<String, Object> newValidationDto(Map<String, Object> dto, String type) {
    return JsonAPITestHelper.toJsonAPIMap("validation", JsonAPITestHelper.toAttributeMap(
      Map.of("type", type, "data", JsonAPITestHelper.toAttributeMap(dto).get("data"))));
  }

  private Map<String, Object> newDepartmentDto() {
    DepartmentDto dto = DepartmentDto.builder().name("dfadf").location("Montreal").build();
    return JsonAPITestHelper.toJsonAPIMap("department", JsonAPITestHelper.toAttributeMap(dto));
  }

  private Map<String, Object> newLongNameDepartmentDto() {
    DepartmentDto dto = DepartmentDto.builder()
      .name("01234567890123456789012345678901234567890123456789a")
      .location("Montreal")
      .build();
    return JsonAPITestHelper.toJsonAPIMap("department", JsonAPITestHelper.toAttributeMap(dto));
  }

  private Map<String, Object> newEmployeeDto() {
    EmployeeDto dto = EmployeeDto.builder()
      .job("01234567890123456789012345678901234567890123456789a")
      .build();
    return JsonAPITestHelper.toJsonAPIMap("employee", JsonAPITestHelper.toAttributeMap(dto));
  }

}
