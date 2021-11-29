package ca.gc.aafc.dina.validation;

import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.dto.ChainDto;
import ca.gc.aafc.dina.dto.DepartmentDto;
import ca.gc.aafc.dina.dto.EmployeeDto;
import ca.gc.aafc.dina.dto.ExternalRelationDto;
import ca.gc.aafc.dina.dto.ValidationDto;
import ca.gc.aafc.dina.testsupport.jsonapi.JsonAPIRelationship;
import ca.gc.aafc.dina.testsupport.jsonapi.JsonAPITestHelper;
import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import org.apache.commons.collections.CollectionUtils;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@SpringBootTest(classes = TestDinaBaseApp.class,
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  properties = {"dev-user.enabled: true", "keycloak.enabled: false", "dina.validationEndpoint.enabled: true"})
public class ValidationRestIT {

  public static final String VALIDATION_TYPE = "validation";
  public static final String VALIDATION_ENDPOINT = "/validation";
  public static final String DEPARTMENT_TYPE = "department";
  public static final String EMPLOYEE_TYPE = "employee";
  @LocalServerPort
  protected int testPort;

  @Test
  void validateLongNameDepartment_ErrorCode422() {
    newRequest()
      .body(newValidationDto(DEPARTMENT_TYPE, newLongNameDepartmentDto(), null))
      .post(VALIDATION_ENDPOINT)
      .then()
      .body("errors[0].status", Matchers.equalToIgnoringCase("422"))
      .body("errors[0].detail", Matchers.endsWith("size must be between 1 and 50"));
  }

  @Test
  void validateInvalidEmployee_ErrorCode422() {
    newRequest()
      .body(newValidationDto(EMPLOYEE_TYPE, newEmployeeDto(), null))
      .post(VALIDATION_ENDPOINT)
      .then()
      .body("errors[0].status", Matchers.equalToIgnoringCase("422"))
      .body("errors[0].detail", Matchers.endsWith("size must be between 1 and 50"));
  }

  @Test
  void validateInvalidEmployee_InvalidPartialISO_ErrorCode422() {
    EmployeeDto employeeDto = EmployeeDto.builder()
      .job("job")
      .employedOn("Twenty Sixteen")
      .build();
    newRequest()
      .body(newValidationDto(EMPLOYEE_TYPE, JsonAPITestHelper.toAttributeMap(employeeDto), null))
      .post(VALIDATION_ENDPOINT)
      .then()
      .body("errors[0].status", Matchers.equalToIgnoringCase("422"))
      .body("errors[0].detail", Matchers.endsWith("The provided value cannot be parsed to ISO Date"));
  }

  @Test
  void validateValidDepartment_Code201() {
    newRequest()
      .body(newValidationDto(DEPARTMENT_TYPE, newDepartmentDto(), null))
      .post(VALIDATION_ENDPOINT)
      .then()
      .assertThat().statusCode(201);
  }

  @Test
  void validate_WithRequiredRelation_Returns201() {
    ChainDto chainDto = new ChainDto();
    chainDto.setGroup("d");
    chainDto.setName("name");
    chainDto.setAgent(ExternalRelationDto.builder().type("agent").id(UUID.randomUUID().toString()).build());
    newRequest()
      .body(newValidationDto(
        "chain",
        JsonAPITestHelper.toAttributeMap(chainDto),
        List.of(JsonAPIRelationship.of("chainTemplate", "chainTemplate", "1"))))
      .post(VALIDATION_ENDPOINT)
      .then()
      .assertThat().statusCode(201);
  }

  @Test
  void validate_WithNoRequiredRelation_Returns422() {
    ChainDto chainDto = new ChainDto();
    chainDto.setGroup("d");
    chainDto.setName("name");
    newRequest()
      .body(newValidationDto("chain", JsonAPITestHelper.toAttributeMap(chainDto), null))
      .post(VALIDATION_ENDPOINT)
      .then()
      .assertThat().statusCode(422);
  }

  @Test
  void validate_WithNoRequiredExternalRelation_Returns422() {
    ChainDto chainDto = new ChainDto();
    chainDto.setGroup("d");
    chainDto.setName("name");
    newRequest()
      .body(newValidationDto(
        "chain",
        JsonAPITestHelper.toAttributeMap(chainDto),
        List.of(JsonAPIRelationship.of("chainTemplate", "chainTemplate", "1"))))
      .post(VALIDATION_ENDPOINT)
      .then()
      .assertThat().statusCode(422);
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "  ", "\t", "\n", "adfadfadfajdlfk", "null"})
  void validate_WhenTypeBlank_ReturnsBadRequest() {
    newRequest()
      .body(newValidationDto("", newDepartmentDto(), null))
      .post(VALIDATION_ENDPOINT)
      .then()
      .assertThat().statusCode(400);
  }

  @Test
  void validate_WhenDataBlank_ReturnsBadRequest() {
    // data: null
    Map<String, Object> jsonAPIMap = JsonAPITestHelper.toJsonAPIMap(
      VALIDATION_TYPE,
      JsonAPITestHelper.toAttributeMap(ValidationDto.builder().type(DEPARTMENT_TYPE).data(null).build()));
    newRequest()
      .body(jsonAPIMap)
      .post(VALIDATION_ENDPOINT)
      .then()
      .assertThat().statusCode(400);
    // data: {}
    jsonAPIMap = JsonAPITestHelper.toJsonAPIMap(VALIDATION_TYPE, JsonAPITestHelper.toAttributeMap(
      Map.of("type", DEPARTMENT_TYPE, "data", Map.of())));
    newRequest()
      .body(jsonAPIMap)
      .post(VALIDATION_ENDPOINT)
      .then()
      .assertThat().statusCode(400);
    // data: { attributes:{} }
    jsonAPIMap = JsonAPITestHelper.toJsonAPIMap(VALIDATION_TYPE, JsonAPITestHelper.toAttributeMap(
      Map.of("type", DEPARTMENT_TYPE, "data", Map.of("attributes", Map.of()))));
    newRequest()
      .body(jsonAPIMap)
      .post(VALIDATION_ENDPOINT)
      .then()
      .assertThat().statusCode(400);
  }

  private RequestSpecification newRequest() {
    return RestAssured.given().port(this.testPort).contentType("application/vnd.api+json");
  }

  private Map<String, Object> newValidationDto(
    String type,
    Map<String, Object> attributes,
    List<JsonAPIRelationship> relations
  ) {
    Map<String, Map<String, Object>> dataMap = new HashMap<>();
    dataMap.put("attributes", JsonAPITestHelper.toAttributeMap(attributes));
    if (CollectionUtils.isNotEmpty(relations)) {
      dataMap.put("relationships", JsonAPITestHelper.toRelationshipMap(relations));
    }
    return JsonAPITestHelper.toJsonAPIMap(VALIDATION_TYPE, Map.of("type", type, "data", dataMap));
  }

  private Map<String, Object> newDepartmentDto() {
    DepartmentDto dto = DepartmentDto.builder()
      .uuid(UUID.randomUUID())
      .name("dfadf")
      .location("Montreal")
      .build();
    return JsonAPITestHelper.toAttributeMap(dto);
  }

  private Map<String, Object> newLongNameDepartmentDto() {
    DepartmentDto dto = DepartmentDto.builder()
      .name("01234567890123456789012345678901234567890123456789a")
      .uuid(UUID.randomUUID())
      .location("Montreal")
      .build();
    return JsonAPITestHelper.toAttributeMap(dto);
  }

  private Map<String, Object> newEmployeeDto() {
    EmployeeDto dto = EmployeeDto.builder()
      .job("01234567890123456789012345678901234567890123456789a")
      .build();
    return JsonAPITestHelper.toAttributeMap(dto);
  }

}
