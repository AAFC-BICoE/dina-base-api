package ca.gc.aafc.dina.testsupport.specs;

import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.dialect.Dialect;
import com.networknt.schema.dialect.Dialects;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpMethod;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedHashSet;
import java.util.List;
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

  private static final ObjectMapper YAML_MAPPER = YAMLMapper.builder().build();
  private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

  private static final String SCHEMA_POINTER_PREFIX = "#/components/schemas/";

  private OpenAPI3Assertions() {
  }

  /**
   * Same as {@link #assertSchema(URL, String, String, ValidationRestrictionOptions)} but the assertion can be skipped by setting
   * the System property {@link #SKIP_REMOTE_SCHEMA_VALIDATION_PROPERTY} to true. Strict mode will be enabled,
   * see {@link #assertRemoteSchema(URL, String, String, ValidationRestrictionOptions)}
   *
   * @param specsUrl    location of the spec
   * @param schemaName  schema name
   * @param apiResponse the api response to assert
   */
  public static void assertRemoteSchema(URL specsUrl, String schemaName, String apiResponse) {
    assertRemoteSchema(specsUrl, schemaName, apiResponse, ValidationRestrictionOptions.FULL_RESTRICTIONS);
  }

  /**
   * Same as {@link #assertSchema(URL, String, String, ValidationRestrictionOptions)} but the assertion can be skipped by setting
   * the System property {@link #SKIP_REMOTE_SCHEMA_VALIDATION_PROPERTY} to true.
   *
   * @param specsUrl    location of the spec
   * @param schemaName  schema name
   * @param apiResponse the api response to assert
   * @param options     validation restriction options to specify allowable missing fields and if additional
   *                    fields are allowed.
   */
  public static void assertRemoteSchema(URL specsUrl, String schemaName, String apiResponse, ValidationRestrictionOptions options) {
    if (!Boolean.valueOf(System.getProperty(SKIP_REMOTE_SCHEMA_VALIDATION_PROPERTY))) {
      assertSchema(specsUrl, schemaName, apiResponse, options);
    } else {
      log.warn("Skipping schema validation. System property testing.skip-remote-schema-validation set to true.");
    }
  }

  /**
   * Assert an API response against an OpenAPI 3 Specification located at specsUrl with strict mode on. See
   * {@link #assertSchema(URL, String, String, ValidationRestrictionOptions)}
   *
   * @param specsUrl    location of the spec
   * @param schemaName  schema name
   * @param apiResponse the api response to assert
   */
  public static void assertSchema(URL specsUrl, String schemaName, String apiResponse) {
    assertSchema(specsUrl, schemaName, apiResponse, ValidationRestrictionOptions.FULL_RESTRICTIONS);
  }

  /**
   * Assert an API response against an OpenAPI 3 Specification located at specsUrl.
   *
   * @param specsUrl    location of the spec
   * @param schemaName  schema name
   * @param apiResponse the api response to assert
   * @param options     validation restriction options to specify allowable missing fields and if additional
   *                    fields are allowed.
   */
  public static void assertSchema(URL specsUrl, String schemaName, String apiResponse, ValidationRestrictionOptions options) {
    Objects.requireNonNull(specsUrl, "specsUrl shall be provided");
    Objects.requireNonNull(schemaName, "schemaName shall be provided");
    Objects.requireNonNull(apiResponse, "apiResponse shall be provided");

    String schemaUri = locateSchema(specsUrl, schemaName);
    if (schemaUri == null) {
      fail("can't find schema " + schemaName);
    }

    JsonNode apiResponseNode;
    try {
      apiResponseNode = JSON_MAPPER.readTree(apiResponse);
    } catch (JacksonException jEx) {
      fail(jEx);
      return;
    }

    Dialect dialect = Dialect.builder(Dialects.getOpenApi30())
      .keyword(RestrictiveFieldValidator.keyword(options))
      .build();
    // fetching is required to resolve file and remote references
    SchemaRegistry registry = SchemaRegistry.withDefaultDialect(dialect,
      builder -> builder.schemaLoader(loader -> loader.fetchRemoteResources()));

    Schema schema = registry.getSchema(SchemaLocation.of(schemaUri));
    List<Error> errors = schema.validate(apiResponseNode);
    if (!errors.isEmpty()) {
      fail(errors.stream().map(Error::toString).collect(Collectors.joining(System.lineSeparator())));
    }
  }

  /**
   * Locate a schema in the main OpenAPI 3 file and, if not found, in the files referenced by the paths.
   *
   * @return the location (URI with fragment) of the schema or null if not found.
   */
  private static String locateSchema(URL specsUrl, String schemaName) {
    JsonNode specs = readSpecs(specsUrl.toString());
    Set<String> candidates = new LinkedHashSet<>();
    candidates.add(specsUrl.toString());
    candidates.addAll(getFilenamesFromPathRefs(specsUrl, specs));

    for (String candidate : candidates) {
      JsonNode content = candidate.equals(specsUrl.toString()) ? specs : readSpecs(candidate);
      if (content.at("/components/schemas").has(schemaName)) {
        return candidate + SCHEMA_POINTER_PREFIX + schemaName;
      }
    }
    return null;
  }

  /**
   * Extract the set of files (absolute URI) referenced from the paths declared in the OpenAPI 3 file.
   */
  private static Set<String> getFilenamesFromPathRefs(URL specsUrl, JsonNode specs) {
    Set<String> filenames = new LinkedHashSet<>();
    specs.path("paths").forEach(pathNode -> {
      String ref = pathNode.path("$ref").asString("");
      String filename = ref.contains("#") ? ref.substring(0, ref.indexOf('#')) : ref;
      if (!filename.isEmpty()) {
        try {
          filenames.add(specsUrl.toURI().resolve(filename).toString());
        } catch (URISyntaxException e) {
          fail("Invalid reference " + ref, e);
        }
      }
    });
    return filenames;
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
    JsonNode paths = readSpecs(specsUrl.toString()).path("paths");
    if (paths.has(path) && paths.get(path).has(method.name().toLowerCase())) {
      return;
    }
    fail("Failed find " + method.name() + " " + path + " in OpenAPI 3 specs");
  }

  private static JsonNode readSpecs(String location) {
    try (InputStream is = new URL(location).openStream()) {
      return YAML_MAPPER.readTree(is);
    } catch (IOException | JacksonException ex) {
      fail("Failed to parse the provided schema", ex);
    }
    return null;
  }

}
