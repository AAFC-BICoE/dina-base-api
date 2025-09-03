package ca.gc.aafc.dina.testsupport;

import static io.restassured.RestAssured.given;

import io.restassured.RestAssured;
import io.restassured.config.EncoderConfig;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import io.restassured.http.Header;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import java.util.Map;

import ca.gc.aafc.dina.jsonapi.JsonApiBulkDocument;
import ca.gc.aafc.dina.jsonapi.JsonApiBulkResourceIdentifierDocument;

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
  public static final String JSON_PATCH_CONTENT_TYPE = "application/json-patch+json";
  public static final String JSON_API_BULK = "application/vnd.api+json; ext=bulk";

  protected static final Header CRNK_HEADER = new Header("crnk-compact", "true");
  private static final String CRNK_OPERATION_ENDPOINT = "operations";

  public static final String JSON_API_BULK_PATH = "bulk";
  public static final String JSON_API_BULK_LOAD_PATH = "bulk-load";

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
      .config(RestAssured.config()
        .encoderConfig(EncoderConfig.encoderConfig()
          .defaultCharsetForContentType("UTF-8", JSON_API_CONTENT_TYPE)
          .defaultCharsetForContentType("UTF-8", JSON_PATCH_CONTENT_TYPE)
          .defaultCharsetForContentType("UTF-8", JSON_API_BULK)))
      .port(testPort)
      .basePath(basePath);
  }

  /**
   * Send GET from a specific identifier from basePath
   * @param id
   * @return
   */
  protected ValidatableResponse sendGet(String id) {
    return sendGet("", id, HttpStatus.OK.value());
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

  protected ValidatableResponse sendGet(String path, String id, Map<String, Object> queryParams,
                                        int expectedReturnCode) {
    var request = newRequest();
    if (queryParams != null) {
      request = request.queryParams(queryParams);
    }

    Response response = request.get(StringUtils.appendIfMissing(path, "/") + "{id}", id);

    return response.then()
      .statusCode(expectedReturnCode);
  }

  /**
   * Send a POST to basePath.
   * @param body
   * @return
   */
  protected ValidatableResponse sendPost(Object body) {
    return sendPost("", body, HttpStatus.CREATED.value());
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
      .log().ifValidationFails()
      .statusCode(expectedReturnCode);
  }

  /**
   * Send a PATCH to basePath.
   * @param id
   * @param body
   * @return
   */
  protected ValidatableResponse sendPatch(String id, Object body) {
    return sendPatch("", id, body, HttpStatus.OK.value());
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
      .log().ifValidationFails()
      .statusCode(expectedReturnCode);
  }

  protected void sendDelete(String path, String id) {
    sendDelete(path, id, HttpStatus.NO_CONTENT.value());
  }

  /**
   * Send DELETE to basePath.
   * @param id
   */
  protected void sendDelete(String id) {
    sendDelete("", id, HttpStatus.NO_CONTENT.value());
  }

  protected void sendDelete(String path, String id, int expectedReturnCode) {
    Response response = newRequest()
      .delete(StringUtils.appendIfMissing(path, "/") + "{id}", id);
    
    response.then().statusCode(expectedReturnCode);
  }

  /**
   * Send a PATCH to the Crnk Operation endpoint.
   * Operation requires all entries to have an id even for POST to uniquely identify them.
   * The id assigned to POST can be any values, it will be changed by the backend.
   * @param body
   * @return
   */
  @Deprecated
  protected ValidatableResponse sendOperation(Object body) {
    Response response = newRequest()
        .accept(JSON_PATCH_CONTENT_TYPE)
        .contentType(JSON_PATCH_CONTENT_TYPE)
        .body(body)
        .patch(CRNK_OPERATION_ENDPOINT);

    return response.then()
        .log().ifValidationFails()
        .statusCode(HttpStatus.OK.value());
  }

  /**
   * Send a bulk-load request to the specified path.
   * bulk-load path will be added.
   * @param path
   * @param body
   * @return
   */
  protected ValidatableResponse sendBulkLoad(String path, JsonApiBulkResourceIdentifierDocument body) {

    Response response = newRequest()
      .accept(JSON_API_CONTENT_TYPE)
      .contentType(JSON_API_BULK)
      .body(body)
      .post(StringUtils.appendIfMissing(path, "/") + JSON_API_BULK_LOAD_PATH);

    return response.then()
      .log().ifValidationFails()
      .statusCode(HttpStatus.OK.value());
  }

  /**
   * Send a bulk create to the specified path.
   * bulk path will be added.
   * @param path
   * @param body
   * @return
   */
  protected ValidatableResponse sendBulkCreate(String path, JsonApiBulkDocument body) {

    Response response = newRequest()
      .accept(JSON_API_CONTENT_TYPE)
      .contentType(JSON_API_BULK)
      .body(body)
      .post(StringUtils.appendIfMissing(path, "/") + JSON_API_BULK_PATH);

    return response.then()
      .log().ifValidationFails()
      .statusCode(HttpStatus.OK.value());
  }

  /**
   * Send a bulk update to the specified path.
   * bulk path will be added.
   * @param path
   * @param body
   * @return
   */
  protected ValidatableResponse sendBulkUpdate(String path, JsonApiBulkDocument body) {

    Response response = newRequest()
      .accept(JSON_API_CONTENT_TYPE)
      .contentType(JSON_API_BULK)
      .body(body)
      .patch(StringUtils.appendIfMissing(path, "/") + JSON_API_BULK_PATH);

    return response.then()
      .log().ifValidationFails()
      .statusCode(HttpStatus.OK.value());
  }

  /**
   * Construct a Builder for URI instances from the provided URI host and URI path
   * 
   * @param host
   * @param specPath
   * @return
   */
  protected static URIBuilder createSchemaUriBuilder(String host, String specPath) {
    URIBuilder uriBuilder = new URIBuilder();
    uriBuilder.setScheme("https");
    uriBuilder.setHost(host);
    uriBuilder.setPath(specPath);
    return uriBuilder;
  }

}
