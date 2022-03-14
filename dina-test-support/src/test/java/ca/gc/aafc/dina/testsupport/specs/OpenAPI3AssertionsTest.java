package ca.gc.aafc.dina.testsupport.specs;

import ca.gc.aafc.dina.testsupport.TestResourceHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.opentest4j.AssertionFailedError;
import org.springframework.http.HttpMethod;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test making sure we can parse, validate and assert a know OpenAPI 3
 * specification with a
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
      ((HttpURLConnection) specsUrl.openConnection()).getResponseCode();
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
  public void assertSchema_WhenNonRequiredAttributeMissing() throws IOException {
    URL specsUrl = this.getClass().getResource("/managedAttribute.yaml");
    String responseJson = TestResourceHelper.readContentAsString("missingAttribute.json");
    Assertions.assertThrows(
        AssertionFailedError.class,
        () -> OpenAPI3Assertions.assertSchema(specsUrl, "ManagedAttribute", responseJson));
  }

  @Test
  public void assertSchema_WhenNonRequiredRelationMissing() throws IOException {
    URL specsUrl = this.getClass().getResource("/managedAttribute.yaml");
    String responseJson = TestResourceHelper.readContentAsString("missingRelation.json");
    Assertions.assertThrows(
        AssertionFailedError.class,
        () -> OpenAPI3Assertions.assertSchema(specsUrl, "ManagedAttribute", responseJson));
  }

  @Test
  public void assertSchema_WhenAttributeShouldNotExist() throws IOException {
    URL specsUrl = this.getClass().getResource("/managedAttribute.yaml");
    String responseJson = TestResourceHelper.readContentAsString("fieldThatShouldNotExist.json");
    Assertions.assertThrows(
        AssertionFailedError.class,
        () -> OpenAPI3Assertions.assertSchema(specsUrl, "ManagedAttribute", responseJson));
  }

  @Test
  public void assertSchema_AllowsAdditionalFields() throws IOException {
    URL specsUrl = this.getClass().getResource("/managedAttribute.yaml");
    String responseJson = TestResourceHelper.readContentAsString("fieldThatShouldNotExist.json");
    OpenAPI3Assertions.assertSchema(specsUrl, "ManagedAttribute", responseJson,
        ValidationRestrictionOptions.builder().allowAdditionalFields(true).build());
  }

  @Test
  public void assertSchema_AllowMissingFields() throws IOException {
    URL specsUrl = this.getClass().getResource("/managedAttribute.yaml");
    String responseJson = TestResourceHelper.readContentAsString("missingAttribute.json");
    OpenAPI3Assertions.assertSchema(specsUrl, "ManagedAttribute", responseJson,
        ValidationRestrictionOptions.builder()
            .allowAdditionalFields(false)
            .allowableMissingFields(Set.of("createdDate", "description", "acceptedValues", "customObject", "name"))
            .build());

    responseJson = TestResourceHelper.readContentAsString("missingRelation.json");
    OpenAPI3Assertions.assertSchema(specsUrl, "ManagedAttribute", responseJson,
        ValidationRestrictionOptions.builder()
            .allowAdditionalFields(true)
            .allowableMissingFields(Set.of("collectingEvent"))
            .build());
  }

  @Test
  public void assertSchema_WhenRelationShouldNotExist() throws IOException {
    URL specsUrl = this.getClass().getResource("/managedAttribute.yaml");
    String responseJson = TestResourceHelper.readContentAsString("relationShouldNotExist.json");
    Assertions.assertThrows(
        AssertionFailedError.class,
        () -> OpenAPI3Assertions.assertSchema(specsUrl, "ManagedAttribute", responseJson));
  }

  @Test
  public void assertRealRemoteSchemaTest() throws IOException {
    String responseJson = TestResourceHelper.readContentAsString("objectStoreManagedAttributeAPIResponse.json");
    OpenAPI3Assertions.assertRemoteSchema(
        new URL("https://raw.githubusercontent.com/DINA-Web/object-store-specs/master/schema/object-store-api.yml"),
        "ManagedAttribute", responseJson);
  }

  @Test
  public void assertSchema_WhenReferencedByPath_ValidationEnabled() throws IOException {
    URL specsUrl = this.getClass().getResource("/customPath.yaml");
    Assertions.assertThrows(
        AssertionFailedError.class,
        () -> OpenAPI3Assertions.assertSchema(specsUrl, "ManagedAttribute",
            TestResourceHelper.readContentAsString("missingAttribute.json")));
    Assertions.assertThrows(
        AssertionFailedError.class,
        () -> OpenAPI3Assertions.assertSchema(specsUrl, "ManagedAttribute",
            TestResourceHelper.readContentAsString("missingRelation.json")));
    OpenAPI3Assertions.assertSchema(specsUrl, "ManagedAttribute",
        TestResourceHelper.readContentAsString("managedAttributeAPIResponse.json"));
  }

  @Test
  public void assertEndPointTest() {
    URL specsUrl = this.getClass().getResource("/managedAttribute.yaml");
    OpenAPI3Assertions.assertEndpoint(specsUrl, "/v1/managed-attribute", HttpMethod.GET);
  }

  @Test
  public void assertSchema_WhenAdditionalFieldInNestedObjectValid_successful() throws IOException {
    // Load in a testing spec.
    URL specsUrl = this.getClass().getResource("/managedAttribute.yaml");

    // Test a valid response.
    String validResponseJson = TestResourceHelper.readContentAsString("additionalFieldInCustomObjectValid.json");
    Assertions.assertDoesNotThrow(
        () -> OpenAPI3Assertions.assertSchema(specsUrl, "ManagedAttribute", validResponseJson));
  }

  @Test
  public void assertSchema_WhenAdditionalFieldInNestedObjectValid_validationException() throws IOException {
    // Load in a testing spec.
    URL specsUrl = this.getClass().getResource("/managedAttribute.yaml");

    // Test an invalid response with contains an additional field in a nest object.
    String invalidResponseJson = TestResourceHelper.readContentAsString("additionalFieldInCustomObjectInvalid.json");
    Assertions.assertThrows(AssertionFailedError.class,
        () -> OpenAPI3Assertions.assertSchema(specsUrl, "ManagedAttribute", invalidResponseJson));
  }
}
