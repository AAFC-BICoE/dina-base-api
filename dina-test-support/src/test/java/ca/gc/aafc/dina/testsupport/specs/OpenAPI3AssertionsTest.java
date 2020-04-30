package ca.gc.aafc.dina.testsupport.specs;

import java.io.IOException;
import java.net.URL;

import org.junit.Test;

import ca.gc.aafc.dina.testsupport.TestResourceHelper;
import ca.gc.aafc.dina.testsupport.specs.OpenAPI3Assertions;

/**
 * Test making sure we can parse, validate and assert a know OpenAPI 3 specification with a
 * predefined API response.
 *
 */
public class OpenAPI3AssertionsTest {

  @Test
  public void assertJsonSchemaTest() throws IOException {
    URL specsUrl = this.getClass().getResource("/managedAttribute.yaml");
    String responseJson = TestResourceHelper.readContentAsString("managedAttributeAPIResponse.json");
    OpenAPI3Assertions.assertSchema(specsUrl, "ManagedAttribute", responseJson);
  }
}
