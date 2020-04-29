package ca.gc.aafc.dina.testsupport.utils;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.openapi4j.core.exception.EncodeException;
import org.openapi4j.core.exception.ResolutionException;
import org.openapi4j.core.model.AuthOption;
import org.openapi4j.core.validation.ValidationException;
import org.openapi4j.core.validation.ValidationResults;
import org.openapi4j.parser.OpenApi3Parser;
import org.openapi4j.parser.model.v3.OpenApi3;
import org.openapi4j.parser.model.v3.Schema;
import org.openapi4j.parser.validation.v3.OpenApi3Validator;
import org.openapi4j.schema.validator.ValidationData;
import org.openapi4j.schema.validator.v3.SchemaValidator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class OpenAPI3SchemaAssertions {
  
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private OpenAPI3SchemaAssertions() {
  }
  
  /**
   * Assert an API response against a OpenAPI 3 Schema.
   * 
   * @param schNode1
   * @param prop
   * @param apiResponse
   */
  public static void assertSchema(JsonNode schNode1, String prop, String apiResponse) {

    SchemaValidator schemaValidator = null;
    try {
      schemaValidator = new SchemaValidator(prop, schNode1);
    } catch (ResolutionException rEx) {
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

  public static JsonNode getJsonNodeForSchemaName(String schemaName, OpenApi3 api1)
      throws Exception {
    JsonNode schemaNode = null;
    Schema schema1 = api1.getComponents().getSchema(schemaName);
    try {
      schemaNode = schema1.toNode(api1.getContext(), true);
    } catch (EncodeException e1) {
      throw new Exception(
          "Error in retrieving data model schema from OpenAPI3 Document. \n" + e1.getMessage());
    }
    return schemaNode;
  }



  public static OpenApi3 parseAndValidateOpenAPI3Doc(URL url1, List<AuthOption> authOptions)
      throws Exception {
    OpenApi3 api = null;
    try {
      api = new OpenApi3Parser().parse(url1, authOptions, false);
    } catch (ResolutionException | ValidationException e) {
      if (e instanceof ResolutionException) {
        throw new Exception(
            "Problem in reaching out to OpenAPI Document - " + url1 + "\n" + e.getMessage());
      }
      if (e instanceof ValidationException) {
        ValidationException ve = (ValidationException) e;
        throw new Exception(ve.getMessage() + "\n" + ve.getResults());
      }
    }
    ValidationResults results = null;
    try {
      results = OpenApi3Validator.instance().validate(api);
    } catch (ValidationException e) {
      ValidationException ve = (ValidationException) e;
      throw new Exception(ve.getMessage() + "\n" + ve.getResults());
    }
    if (!results.toString().trim().equals("")) {
      String errors = results.toString();
      if (errors.length() > 0) {
        throw new Exception(errors);
      }
    }
    return api;
  }

}
