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
 * specification with a predefined API response.
 */
public class OpenAPI3AssertionsTest {

  private static final String REMOTE_SPECS = "https://raw.githubusercontent.com/DINA-Web/object-store-specs/master/schema/object-store-api.yml";

  /**
   * Test making sure we can skip a schema validation by a system property.
   * To simulate an unreachable URL we are simply using a non-existing url.
   */
  @Test
  public void assertRemoteSchema_InvalidURLWithSkipRemoteValidation_NoValidationIssues() throws IOException {

    URL specsUrl = new URL("http://abcd.abc");
    assertThrows(UnknownHostException.class, () -> {
      ((HttpURLConnection) specsUrl.openConnection()).getResponseCode();
    }, "Make sure specsUrl doesn't exist and is not reachable.");

    System.setProperty(OpenAPI3Assertions.SKIP_REMOTE_SCHEMA_VALIDATION_PROPERTY, "true");
    String responseJson = TestResourceHelper.readContentAsString("managedAttributeAPIResponse.json");
    OpenAPI3Assertions.assertRemoteSchema(specsUrl, "ManagedAttribute", responseJson);
    System.clearProperty(OpenAPI3Assertions.SKIP_REMOTE_SCHEMA_VALIDATION_PROPERTY);
  }

  /**
   * This test will actually use the object-store-api schema to validate a response. This test is
   * used to indicate remote schema validation is working.
   */
  @Test
  public void assertRemoteSchema_ValidURLProvided_NoValidationIssues() throws IOException {
    String responseJson = TestResourceHelper.readContentAsString("objectStoreManagedAttributeAPIResponse.json");
    OpenAPI3Assertions.assertRemoteSchema(
        new URL(REMOTE_SPECS),
        "ManagedAttribute", responseJson);
  }

  /**
   * Test to ensure the assertEndPoint is validating the HttpMethod of the endpoint without any
   * issues.
   */
  @Test
  public void assertEndPoint_ValidEndpoint_Successful() {
    URL specsUrl = this.getClass().getResource("/managedAttribute.yaml");
    OpenAPI3Assertions.assertEndpoint(specsUrl, "/v1/managed-attribute", HttpMethod.GET);
  }

  /**
   * Test a valid json response against a schema. No validation exception should be provided.
   */
  @Test
  public void assertSchema_CorrectResponse_NoValidationIssues() throws IOException {
    URL specsUrl = this.getClass().getResource("/managedAttribute.yaml");
    String responseJson = TestResourceHelper.readContentAsString("managedAttributeAPIResponse.json");
    OpenAPI3Assertions.assertSchema(specsUrl, "ManagedAttribute", responseJson);
  }

  /**
   * All fields provided on the schema are considered required by design. This test will purposely
   * remove a required field from the response to ensure an error is retrieved.
   */
  @Test
  public void assertSchema_WhenNonRequiredAttributeMissing_ExpectValidationIssues() throws IOException {
    URL specsUrl = this.getClass().getResource("/managedAttribute.yaml");
    String responseJson = TestResourceHelper.readContentAsString("missingAttribute.json");
    Assertions.assertThrows(
        AssertionFailedError.class,
        () -> OpenAPI3Assertions.assertSchema(specsUrl, "ManagedAttribute", responseJson));
  }

  /**
   * All relationships provided on the schema are considered required by design. This test will
   * purposely remove the "collectingEvent" relationship. A validation error is to be expected.
   */
  @Test
  public void assertSchema_WhenNonRequiredRelationMissing_ExpectValidationIssues() throws IOException {
    URL specsUrl = this.getClass().getResource("/managedAttribute.yaml");
    String responseJson = TestResourceHelper.readContentAsString("missingRelation.json");
    Assertions.assertThrows(
        AssertionFailedError.class,
        () -> OpenAPI3Assertions.assertSchema(specsUrl, "ManagedAttribute", responseJson));
  }

  /**
   * Any additional fields that are not specified in the schema are to be reported.
   */
  @Test
  public void assertSchema_WhenAttributeShouldNotExist_ExpectValidationIssues() throws IOException {
    URL specsUrl = this.getClass().getResource("/managedAttribute.yaml");
    String responseJson = TestResourceHelper.readContentAsString("fieldThatShouldNotExist.json");
    Assertions.assertThrows(
        AssertionFailedError.class,
        () -> OpenAPI3Assertions.assertSchema(specsUrl, "ManagedAttribute", responseJson));
  }

  /**
   * With the allowAdditionalFields setting, any included fields which are not specified in the
   * schema are ignored. No errors are expected here.
   */
  @Test
  public void assertSchema_AllowsAdditionalFields_NoValidationIssues() throws IOException {
    URL specsUrl = this.getClass().getResource("/managedAttribute.yaml");
    String responseJson = TestResourceHelper.readContentAsString("fieldThatShouldNotExist.json");
    OpenAPI3Assertions.assertSchema(specsUrl, "ManagedAttribute", responseJson,
        ValidationRestrictionOptions.builder().allowAdditionalFields(true).build());
  }

  /**
   * All fields in a schema are expected in the response by design. However, using the
   * allowableMissingFields you can specify a path to a field that can be missing.
   */
  @Test
  public void assertSchema_AllowMissingFields_NoValidationIssues() throws IOException {
    URL specsUrl = this.getClass().getResource("/managedAttribute.yaml");
    String responseJson = TestResourceHelper.readContentAsString("missingAttribute.json");
    OpenAPI3Assertions.assertSchema(specsUrl, "ManagedAttribute", responseJson,
        ValidationRestrictionOptions.builder()
            .allowAdditionalFields(false)
            .allowableMissingFields(Set.of("createdDate", "description", "acceptedValues", "customObject"))
            .build());

    responseJson = TestResourceHelper.readContentAsString("missingRelation.json");
    OpenAPI3Assertions.assertSchema(specsUrl, "ManagedAttribute", responseJson,
        ValidationRestrictionOptions.builder()
            .allowAdditionalFields(true)
            .allowableMissingFields(Set.of("collectingEvent"))
            .build());
  }

  /**
   * The validator should report a additional relationship field provided that is not specified on
   * the schema. Validation error is expected here.
   */
  @Test
  public void assertSchema_WhenRelationShouldNotExist_ExpectValidationIssues() throws IOException {
    URL specsUrl = this.getClass().getResource("/managedAttribute.yaml");
    String responseJson = TestResourceHelper.readContentAsString("relationShouldNotExist.json");
    Assertions.assertThrows(
        AssertionFailedError.class,
        () -> OpenAPI3Assertions.assertSchema(specsUrl, "ManagedAttribute", responseJson));
  }

  /**
   * Test the validators ability to use referenced paths. The customPath.yaml specifies multiple
   * different paths that go to different yaml files. Validation should still work when these
   * paths are being used.
   */
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

  /**
   * Test with nested objects. This test contains the completed nested object in the response. No
   * validation issues should occur.
   */
  @Test
  public void assertSchema_WhenAdditionalFieldInNestedObjectValid_Successful() throws IOException {
    // Load in a testing spec.
    URL specsUrl = this.getClass().getResource("/managedAttribute.yaml");

    // Test a valid response.
    String validResponseJson = TestResourceHelper.readContentAsString("additionalFieldInCustomObjectValid.json");
    Assertions.assertDoesNotThrow(
        () -> OpenAPI3Assertions.assertSchema(specsUrl, "ManagedAttribute", validResponseJson));
  }

  /**
   * Test with nested objects. This test contains an incomplete nested object in the response.
   * Validation issues should occur, this will ensure the validator is going through all levels of
   * the response/schema.
   */
  @Test
  public void assertSchema_WhenAdditionalFieldInNestedObjectValid_ValidationException() throws IOException {
    // Load in a testing spec.
    URL specsUrl = this.getClass().getResource("/managedAttribute.yaml");

    // Test an invalid response with contains an additional field in a nest object.
    String invalidResponseJson = TestResourceHelper.readContentAsString("additionalFieldInCustomObjectInvalid.json");
    Assertions.assertThrows(AssertionFailedError.class,
        () -> OpenAPI3Assertions.assertSchema(specsUrl, "ManagedAttribute", invalidResponseJson));
  }
}
