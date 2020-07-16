package ca.gc.aafc.dina.testsupport.specs;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Objects;

import org.openapi4j.core.exception.EncodeException;
import org.openapi4j.core.exception.ResolutionException;
import org.openapi4j.core.validation.ValidationException;
import org.openapi4j.parser.OpenApi3Parser;
import org.openapi4j.parser.model.v3.OpenApi3;
import org.openapi4j.parser.model.v3.Schema;
import org.openapi4j.parser.validation.v3.OpenApi3Validator;
import org.openapi4j.schema.validator.ValidationData;
import org.openapi4j.schema.validator.v3.SchemaValidator;

import lombok.extern.log4j.Log4j2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Collections of utility test methods related to OpenAPI 3 specifications and
 * schemas.
 *
 */
@Log4j2
public final class OpenAPI3Assertions {

  public static final String SKIP_REMOTE_SCHEMA_VALIDATION_PROPERTY = "testing.skip-remote-schema-validation";

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private OpenAPI3Assertions() {
  }

  /**
   * Same as {@link #assertSchema(URL, String, String)} but the assertion can be
   * skipped by setting the System property
   * {@link #SKIP_REMOTE_SCHEMA_VALIDATION_PROPERTY} to true.
   * 
   * @param specsUrl
   * @param schemaName
   * @param apiResponse
   */
  public static void assertRemoteSchema(URL specsUrl, String schemaName, String apiResponse, String resourceaName) {
    if (!Boolean.valueOf(System.getProperty(SKIP_REMOTE_SCHEMA_VALIDATION_PROPERTY))) {
      assertSchema(specsUrl, schemaName, apiResponse, resourceaName);
    } else {
      log.warn("Skipping schema validation." + "System property testing.skip-remote-schema-validation set to true.");
    }
  }

  /**
   * Assert an API response against an OpenAPI 3 Specification located at
   * specsUrl.
   * 
   * @param specsUrl
   * @param schemaName
   * @param apiResponse
   */
  public static void assertSchema(URL specsUrl, String schemaName, String apiResponse, String resourceName) {
    Objects.requireNonNull(specsUrl, "specsUrl shall be provided");
    Objects.requireNonNull(schemaName, "schemaName shall be provided");
    Objects.requireNonNull(apiResponse, "apiResponse shall be provided");

    OpenApi3 openApi = null;
    try {
      openApi = parseAndValidateOpenAPI3Specs(specsUrl);
    } catch (ResolutionException | ValidationException ex) {
      fail("Failed to parse and validate the provided schema", ex);
      return;
    }

    if (!assertPaths(openApi, resourceName)) {
      fail("Failed to parse paths for resource: "+ resourceName);      
          return;
    }
          
    assertSchema(openApi, schemaName, apiResponse);
  }

  /**
   * Assert an API response against the provided OpenAPI 3 Specification.
   * 
   * @param schNode1
   * @param prop
   * @param apiResponse
   */
  public static void assertSchema(OpenApi3 openApi, String schemaName, String apiResponse) {

    SchemaValidator schemaValidator = null;
    try {
      JsonNode schemaNode = loadSchemaAsJsonNode(openApi, schemaName);
      schemaValidator = new SchemaValidator(null, schemaNode);
    } catch (ResolutionException | EncodeException rEx) {
      fail(rEx);
      return;
    }

    JsonNode apiResponseNode = null;
    try {
      apiResponseNode = MAPPER.readTree(apiResponse);
    } catch (IOException ioEx) {
      fail(ioEx);
      return;
    }

    ValidationData<?> validationData = new ValidationData<>();
    schemaValidator.validate(apiResponseNode, validationData);

    if (!validationData.isValid()) {
      fail(validationData.results().toString());
      return;
    }
  }

  /**
   * Load a schema inside an OpenApi3 object.
   * 
   * @param specUrl
   * @param schemaName
   * @return the schema as {@link JsonNode}
   * @throws EncodeException
   */
  private static JsonNode loadSchemaAsJsonNode(OpenApi3 openApi, String schemaName) throws EncodeException {
    Schema schema = openApi.getComponents().getSchema(schemaName);
    return schema.toNode(openApi.getContext(), true);
  }

  /**
   * Parse and validate the OpenAPI 3 specifications at the provided URL.
   * 
   * @param schemaURL
   * @return the OpenApi3 as {@link OpenApi3}
   * @throws ValidationException
   * @throws ResolutionException
   */
  public static OpenApi3 parseAndValidateOpenAPI3Specs(URL specsURL) throws ResolutionException, ValidationException {

    OpenApi3 api = new OpenApi3Parser().parse(specsURL, new ArrayList<>(), false);
    OpenApi3Validator.instance().validate(api);

    return api;
  }
  
  /**
   * Checking that each path contains resourceName
   * 
   * @param api3
   * @param resourceName
   * @return true if all paths contain resourceName; false if any path does not contain resourceName 
   */
  private static boolean assertPaths(OpenApi3 api3, String resourceName) {
    boolean resourceInPath = true;
    for (String key : api3.getPaths().keySet()) {
      if (!key.contains(resourceName)) {
        resourceInPath = false;
        break;
      }
    }
    return resourceInPath;
  }  

}
