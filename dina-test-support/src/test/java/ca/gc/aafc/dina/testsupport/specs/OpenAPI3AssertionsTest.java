package ca.gc.aafc.dina.testsupport.specs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;

import org.junit.Test;
import org.springframework.http.HttpMethod;

import ca.gc.aafc.dina.testsupport.TestResourceHelper;

/**
 * Test making sure we can parse, validate and assert a know OpenAPI 3 specification with a
 * predefined API response.
 *
 */
public class OpenAPI3AssertionsTest {

  /**
   * Test making sure we can skip a schema validation by a system property.
   * To simulate an ureachable URL we are simply using a non-existing url.
   */
  @Test
  public void assertRemoteSchemaTest() throws IOException {

    URL specsUrl = new URL("http://abcd.abc");
    assertThrows(UnknownHostException.class, () -> {
      ((HttpURLConnection)specsUrl.openConnection()).getResponseCode();
    }, "Make sure specsUrl doesn't exist and is not reachable.");

    System.setProperty(OpenAPI3Assertions.SKIP_REMOTE_SCHEMA_VALIDATION_PROPERTY, "true");
    String responseJson = TestResourceHelper.readContentAsString("managedAttributeAPIResponse.json");
    OpenAPI3Assertions.assertRemoteSchema(specsUrl, "ManagedAttribute", responseJson);
    System.clearProperty(OpenAPI3Assertions.SKIP_REMOTE_SCHEMA_VALIDATION_PROPERTY);
  }

  @Test
  public void assertSchemaTest() throws IOException {
    URL specsUrl = this.getClass().getResource("/managedAttribute.yaml");
    String responseJson = TestResourceHelper.readContentAsString("managedAttributeAPIResponse.json");
    OpenAPI3Assertions.assertSchema(specsUrl, "ManagedAttribute", responseJson);
  }
  
  @Test
  public void assertEndPointTest() throws IOException {
    URL specsUrl = this.getClass().getResource("/managedAttribute.yaml");
    OpenAPI3Assertions.assertEndpoint(specsUrl, "/v1/managed-attribute",HttpMethod.GET);
  }  

}
