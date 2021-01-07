package ca.gc.aafc.dina.jpa;

import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.dto.DepartmentDto;
import ca.gc.aafc.dina.testsupport.BaseRestAssuredTest;
import ca.gc.aafc.dina.testsupport.jsonapi.JsonAPITestHelper;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
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
  void validate_OnDifferentLanguage_LanguageTranslated() {
    DepartmentDto dto = DepartmentDto.builder().name("dfadf").location(null).build();

    Map<String, Object> map = JsonAPITestHelper.toJsonAPIMap(
      "department",
      JsonAPITestHelper.toAttributeMap(dto));

    RestAssured.given()
      .port(this.testPort)
      .contentType("application/vnd.api+json")
      .header("lang", "fr")
      .body(map)
      .post("/department")
      .then().log().all(true);
  }
}
