package ca.gc.aafc.dina.testsupport.specs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.openapi4j.core.exception.EncodeException;
import org.openapi4j.core.exception.ResolutionException;
import org.openapi4j.core.model.reference.Reference;
import org.openapi4j.core.model.v3.OAI3;
import org.openapi4j.core.validation.ValidationException;
import org.openapi4j.parser.OpenApi3Parser;
import org.openapi4j.parser.model.v3.OpenApi3;
import org.openapi4j.parser.model.v3.Schema;
import org.openapi4j.parser.validation.v3.OpenApi3Validator;
import org.openapi4j.schema.validator.ValidationContext;
import org.openapi4j.schema.validator.ValidationData;
import org.openapi4j.schema.validator.v3.SchemaValidator;
import org.springframework.http.HttpMethod;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Collections of utility test methods related to OpenAPI 3 specifications and schemas.
 */
@Log4j2
public final class OpenAPI3Assertions {

  public static final String SKIP_REMOTE_SCHEMA_VALIDATION_PROPERTY = "testing.skip-remote-schema-validation";

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private OpenAPI3Assertions() {
  }

  /**
   * Same as {@link #assertSchema(URL, String, String, boolean)} but the assertion can be skipped by setting
   * the System property {@link #SKIP_REMOTE_SCHEMA_VALIDATION_PROPERTY} to true. Strict mode will be enabled,
   * see {@link #assertRemoteSchema(URL, String, String, boolean)}
   *
   * @param specsUrl    location of the spec
   * @param schemaName  schema name
   * @param apiResponse the api response to assert
   */
  public static void assertRemoteSchema(URL specsUrl, String schemaName, String apiResponse) {
    assertRemoteSchema(specsUrl, schemaName, apiResponse, true);
  }

  /**
   * Same as {@link #assertSchema(URL, String, String, boolean)} but the assertion can be skipped by setting
   * the System property {@link #SKIP_REMOTE_SCHEMA_VALIDATION_PROPERTY} to true.
   *
   * @param specsUrl    location of the spec
   * @param schemaName  schema name
   * @param apiResponse the api response to assert
   * @param strictMode  strict mode will make all the fields from the schema required to make sure a complete
   *                    api response is valid.
   */
  public static void assertRemoteSchema(URL specsUrl, String schemaName, String apiResponse, boolean strictMode) {
    if (!Boolean.valueOf(System.getProperty(SKIP_REMOTE_SCHEMA_VALIDATION_PROPERTY))) {
      assertSchema(specsUrl, schemaName, apiResponse, strictMode);
    } else {
      log.warn("Skipping schema validation." + "System property testing.skip-remote-schema-validation set to true.");
    }
  }

  /**
   * Assert an API response against an OpenAPI 3 Specification located at specsUrl with strict mode on. See
   * {@link #assertSchema(OpenApi3, String, String, boolean)}
   *
   * @param specsUrl    location of the spec
   * @param schemaName  schema name
   * @param apiResponse the api response to assert
   */
  public static void assertSchema(URL specsUrl, String schemaName, String apiResponse) {
    assertSchema(specsUrl, schemaName, apiResponse, true);
  }

  /**
   * Assert an API response against an OpenAPI 3 Specification located at specsUrl.
   *
   * @param specsUrl    location of the spec
   * @param schemaName  schema name
   * @param apiResponse the api response to assert
   * @param strictMode strict mode will make all the fields from the schema required to make sure a
   *                   complete api response is valid.
   */
  public static void assertSchema(URL specsUrl, String schemaName, String apiResponse, boolean strictMode) {
    Objects.requireNonNull(specsUrl, "specsUrl shall be provided");
    Objects.requireNonNull(schemaName, "schemaName shall be provided");
    Objects.requireNonNull(apiResponse, "apiResponse shall be provided");

    OpenApi3 openApi3 = innerParseAndValidateOpenAPI3Specs(specsUrl) ;
    assertSchema(openApi3, schemaName, apiResponse, strictMode);
  }

  /**
   * Assert an API response against the provided OpenAPI 3 Specification with strict mode on. See {@link
   * #assertSchema(OpenApi3, String, String, boolean)}
   *
   * @param openApi     provided specification
   * @param schemaName  schema name
   * @param apiResponse the api response to assert
   */
  public static void assertSchema(OpenApi3 openApi, String schemaName, String apiResponse) {
    assertSchema(openApi, schemaName, apiResponse, true);
  }

  /**
   * Assert an API response against the provided OpenAPI 3 Specification.
   *
   * @param openApi     provided specification
   * @param schemaName  schema name
   * @param apiResponse the api response to assert
   * @param strictMode  strict mode will make all the fields from the schema required to make sure a complete
   *                    api response is valid.
   */
  public static void assertSchema(OpenApi3 openApi, String schemaName, String apiResponse, boolean strictMode) {

    SchemaValidator schemaValidator;
    try {
      ValidationContext<OAI3> context = new ValidationContext<>(openApi.getContext());
      JsonNode schemaNode = loadSchemaAsJsonNode(openApi, schemaName, strictMode);
      if (schemaNode == null ) {
        fail("can't find schema " + schemaName);
      }
      schemaValidator = new SchemaValidator(context, null, schemaNode);
    } catch (EncodeException rEx) {
      fail(rEx);
      return;
    }

    JsonNode apiResponseNode;
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
    }
  }

  /**
   * Load a schema inside an OpenApi3 object.
   *
   * @param openApi
   * @param schemaName
   * @param strictMode strict mode will make all the fields from the schema required to make sure a
   *                   complete api response is valid.
   * @return the schema as {@link JsonNode}
   * @throws EncodeException
   */
  private static JsonNode loadSchemaAsJsonNode(OpenApi3 openApi, String schemaName, boolean strictMode)
      throws EncodeException {

    // try to locate the schema in the main OpenAPI3 file
    if (openApi.getComponents() != null) {
      Schema schema = openApi.getComponents().getSchema(schemaName);
      if (schema != null) {
        if (strictMode) {
          setAllFieldsRequired(schema);
        }
        return schema.toNode();
      }
    }

    // then, try to reach it be refs from the paths
    Set<String> filenamesFromPathRefs = getFilenamesFromPathRefs(openApi);
    return loadFirstFoundSchema(openApi, filenamesFromPathRefs, schemaName);
  }

  private static void setAllFieldsRequired(@NonNull Schema schema) {
    Map<String, Schema> properties = schema.getProperties();
    if (properties != null && properties.containsKey("data")) {
      Schema data = properties.get("data");

      // if nothing to set just exit
      if (data == null || data.getProperties() == null) {
        return;
      }

      if (data.getProperties().containsKey("attributes")) {
        Schema attributes = data.getProperties().get("attributes");
        attributes.setAdditionalPropertiesAllowed(false);
        attributes.setRequiredFields(new ArrayList<>(attributes.getProperties().keySet()));
      }

      if (data.getProperties().containsKey("relationships")) {
        data.addRequiredField("relationships");
        Schema relations = data.getProperties().get("relationships");
        relations.setAdditionalPropertiesAllowed(false);
        relations.setRequiredFields(new ArrayList<>(relations.getProperties().keySet()));
      }

    }
  }

  /**
   * Extract a set of filenames from the list of Paths declared in the OpenApi3 file
   * @param openApi
   * @return set of paths or empty set if no paths
   */
  private static Set<String> getFilenamesFromPathRefs(OpenApi3 openApi) {
    if (openApi.getPaths() == null) {
      return Collections.emptySet();
    }
    return openApi.getPaths().values().stream()
        .map(path -> StringUtils.substringBefore(path.getRef(), "#"))
        .collect(Collectors.toSet());
  }

  /**
   * From a set of filenames, return the first content that can be located.
   * @param openApi
   * @param filenames
   * @param schemaName
   * @return content as JsonNode or null if not found.
   */
  private static JsonNode loadFirstFoundSchema(OpenApi3 openApi, Set<String> filenames,
      String schemaName) {
    Reference ref;
    for (String filename : filenames) {
      ref = openApi.getContext().getReferenceRegistry()
          .getRef(filename + "#/components/schemas/" + schemaName);
      if (ref != null) {
        return ref.getContent();
      }
    }
    return null;
  }

  /**
   * Parse and validate the OpenAPI 3 specifications at the provided URL.
   *
   * @param specsURL
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
   * Checking the given path is existing in Open API 3 spec
   * and the path contains the provided http method
   *
   * @param specsUrl Specs URL to check endpoints against
   * @param path     path of endpoint
   * @param method   method at the endpoint path
   */
  public static void assertEndpoint(URL specsUrl, String path, HttpMethod method) {
    OpenApi3 openApi3 = innerParseAndValidateOpenAPI3Specs(specsUrl);
    for (String key : openApi3.getPaths().keySet()) {
      if (key.equals(path)
          && openApi3.getPaths().get(key).getOperation(method.name().toLowerCase()) != null) {
        return;
      }
    }
    fail("Failed find " + method.name() + " " + path + " in OpenAPI 3 specs");
  }

  private static OpenApi3 innerParseAndValidateOpenAPI3Specs(URL specsUrl) {
    try {
      return parseAndValidateOpenAPI3Specs(specsUrl);
    } catch (ResolutionException | ValidationException ex) {
      fail("Failed to parse and validate the provided schema", ex);
    }
    return null;
  }

}
