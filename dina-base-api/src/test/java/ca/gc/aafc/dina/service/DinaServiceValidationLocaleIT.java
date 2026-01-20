package ca.gc.aafc.dina.service;

import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.config.PersonTestConfig;
import ca.gc.aafc.dina.dto.DepartmentDto;
import ca.gc.aafc.dina.dto.PersonDTO;
import ca.gc.aafc.dina.testsupport.PostgresTestContainerInitializer;
import ca.gc.aafc.dina.testsupport.jsonapi.JsonAPITestHelper;
import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ContextConfiguration;

import java.util.Map;

import static com.toedter.spring.hateoas.jsonapi.MediaTypes.JSON_API_VALUE;

@SpringBootTest(classes = {TestDinaBaseApp.class, PersonTestConfig.class},
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  properties = {"dev-user.enabled: true", "keycloak.enabled: false"})
@ContextConfiguration(initializers = { PostgresTestContainerInitializer.class })
public class DinaServiceValidationLocaleIT {

  @LocalServerPort
  protected int testPort;

  @Test
  void validate_OnDifferentLocale_DifferentLocaleRegularValidationUsed() {
    newRequest()
      .header("Accept-Language", "fr") //this is not working anymore
      .body(newLongNameDto())
      .post( DepartmentDto.TYPE_NAME + "?lang=fr")
      .then()
      .body("errors[0].code", Matchers.equalToIgnoringCase("422"))
      .body("errors[0].detail", Matchers.endsWith("name la taille doit être comprise entre 1 et 50"));
  }

  @Test
  void validate_OnDifferentLocale_DifferentLocaleUsed() {
    newRequest()
      .header("Accept-Language", "fr")
      .body(newDto())
      .post(DepartmentDto.TYPE_NAME + "?lang=fr")
      .then()
      .body("errors[0].code", Matchers.equalToIgnoringCase("422"))
      .body("errors[0].detail", Matchers.endsWith("location ne peut pas être nul."));
  }

  @Test
  void validate_OnDefaultLocale_DefaultLocaleUsed() {
    newRequest()
      .body(newDto())
      .post(DepartmentDto.TYPE_NAME)
      .then()
      .body("errors[0].code", Matchers.equalToIgnoringCase("422"))
      .body("errors[0].detail", Matchers.endsWith("location cannot be null. Single quote test'."));
  }

  private RequestSpecification newRequest() {
    return RestAssured.given().port(this.testPort).contentType(JSON_API_VALUE);
  }

  private Map<String, Object> newDto() {
    DepartmentDto dto = DepartmentDto.builder().name("dfadf").location(null).build();
    return JsonAPITestHelper.toJsonAPIMap(DepartmentDto.TYPE_NAME, JsonAPITestHelper.toAttributeMap(dto));
  }
  private Map<String, Object> newPersonWithLongNameDto() {
    PersonDTO dto = PersonDTO.builder().name("01234567890123456789012345678901234567890123456789a").build();
    return JsonAPITestHelper.toJsonAPIMap(PersonDTO.TYPE_NAME, JsonAPITestHelper.toAttributeMap(dto));
  }

  private Map<String, Object> newPersonDto() {
    PersonDTO dto = PersonDTO.builder().name("sdgjfga").build();
    return JsonAPITestHelper.toJsonAPIMap(PersonDTO.TYPE_NAME, JsonAPITestHelper.toAttributeMap(dto));
  }

  private Map<String, Object> newLongNameDto() {
    DepartmentDto dto = DepartmentDto.builder().name("01234567890123456789012345678901234567890123456789a").location("Montreal").build();
    return JsonAPITestHelper.toJsonAPIMap(DepartmentDto.TYPE_NAME, JsonAPITestHelper.toAttributeMap(dto));
  }
}