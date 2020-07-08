package ca.gc.aafc.dina.testsupport;

import static io.restassured.RestAssured.given;

import javax.transaction.Transactional;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import io.restassured.http.Header;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import lombok.extern.log4j.Log4j2;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
@Log4j2
public class BaseRestAssuredTest {

  public static final String JSON_API_CONTENT_TYPE = "application/vnd.api+json";

  private static final Header CRNK_HEADER = new Header("crnk-compact", "true");

  @LocalServerPort
  protected int testPort;

  protected final String basePath;

  /**
   * 
   * @param basePath path to reach the resource under test. Could be empty if the test should
   * call the root.
   */
  protected BaseRestAssuredTest(String basePath) {
    this.basePath = basePath;
  }

  protected ValidatableResponse sendGet(String path, String id, int expectedReturnCode) {
    Response response = given()
      .header(CRNK_HEADER)
      .when()
      .port(testPort)
      .basePath(basePath)
      .get(path + "/" + id);

    return response.then()
        .statusCode(expectedReturnCode);
  }

}
