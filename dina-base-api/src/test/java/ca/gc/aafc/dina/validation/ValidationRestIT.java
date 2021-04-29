package ca.gc.aafc.dina.validation;

import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;

import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.dto.DepartmentDto;
import ca.gc.aafc.dina.dto.EmployeeDto;
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

  private RequestSpecification newRequest() {
    return RestAssured.given().port(this.testPort).contentType("application/vnd.api+json");
  }

  private Map<String, Object> newValidationDto(Map<String, Object> dto, String type) {
    ValidationDto validationDto = ValidationDto.builder().type(type).data(dto).build();
    return JsonAPITestHelper.toJsonAPIMap("validation", JsonAPITestHelper.toAttributeMap(validationDto));
  }
  
  private Map<String, Object> newDepartmentDto() {
    DepartmentDto dto = DepartmentDto.builder().name("dfadf").location("Montreal").build();
    return JsonAPITestHelper.toJsonAPIMap("department", JsonAPITestHelper.toAttributeMap(dto));
  }

  private Map<String, Object> newLongNameDepartmentDto() {
    DepartmentDto dto = DepartmentDto.builder().name("01234567890123456789012345678901234567890123456789a").location("Montreal").build();
    return JsonAPITestHelper.toJsonAPIMap("department", JsonAPITestHelper.toAttributeMap(dto));
  }

  private Map<String, Object> newEmployeeDto() {
    EmployeeDto dto = EmployeeDto.builder().job("01234567890123456789012345678901234567890123456789a").build();
    return JsonAPITestHelper.toJsonAPIMap("employee", JsonAPITestHelper.toAttributeMap(dto));
  }

}
