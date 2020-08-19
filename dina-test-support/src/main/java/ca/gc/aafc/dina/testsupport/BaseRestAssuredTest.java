package ca.gc.aafc.dina.testsupport;

import static io.restassured.RestAssured.given;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import io.restassured.http.Header;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;

/**
 * Base class for RestAssured integration tests.
 * It will start a webserver on a random port and send operations to it.
 * 
 * When this class is extended the SpringBootTest annotation should be re-added in order to add a configuration
 * class but also keep the webEnvironment.
 * 
 * <pre>
 * {@literal @}SpringBootTest(classes = TestConfiguration.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
 * </pre>
 * 
 * All operations will validate the return code and the expected return code can be
 * provided to replace the default value.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class BaseRestAssuredTest {

  public static final String JSON_API_CONTENT_TYPE = "application/vnd.api+json";

  private static final Header CRNK_HEADER = new Header("crnk-compact", "true");

  @LocalServerPort
  protected int testPort;

  protected final String basePath;

  /**
   * Main constructor where the basePath is specified.
   * basePath can be an empty string if the test should run from the root (ex. localhost:9999).
   * For Crnk based app, it should match crnk.pathPrefix .
   * @param basePath path to reach the resource under test. Could be empty if the test should
   * call the root.
   */
  protected BaseRestAssuredTest(String basePath) {
    this.basePath = basePath == null ? "" : basePath;
  }

  /**
   * Prepare a new, fresh, request ready to be used.
   * @return
   */
  private RequestSpecification newRequest() {
    return given()
      .header(CRNK_HEADER)
      .port(testPort)
      .basePath(basePath);
  }

  protected ValidatableResponse sendGet(String path, String id) {
    return sendGet(path, id, HttpStatus.OK.value());
  }

  protected ValidatableResponse sendGet(String path, String id, int expectedReturnCode) {
    Response response = newRequest()
      .get(StringUtils.appendIfMissing(path, "/") + "{id}", id);

    return response.then()
      .statusCode(expectedReturnCode);
  }

  protected ValidatableResponse sendPost(String path, Object body) {
    return sendPost(path, body, HttpStatus.CREATED.value());
  }

  protected ValidatableResponse sendPost(String path, Object body, int expectedReturnCode) {
    Response response =  newRequest()
      .contentType(JSON_API_CONTENT_TYPE)
      .body(body)
      .post(path);

    return response.then()
      .statusCode(expectedReturnCode);
  }

  protected ValidatableResponse sendPatch(String path, String id, Object body) {
    return sendPatch(path, id, body, HttpStatus.OK.value());
  }

  protected ValidatableResponse sendPatch(String path, String id, Object body, int expectedReturnCode) {
    Response response =  newRequest()
      .contentType(JSON_API_CONTENT_TYPE)
      .body(body)
      .patch(StringUtils.appendIfMissing(path, "/") + "{id}", id);

    return response.then()
      .statusCode(expectedReturnCode);
  }

  protected void sendDelete(String path, String id) {
    sendDelete(path, id, HttpStatus.NO_CONTENT.value());
  }

  protected void sendDelete(String path, String id, int expectedReturnCode) {
    Response response = newRequest()
      .delete(StringUtils.appendIfMissing(path, "/") + "{id}", id);
    
    response.then().statusCode(expectedReturnCode);
  }

}
