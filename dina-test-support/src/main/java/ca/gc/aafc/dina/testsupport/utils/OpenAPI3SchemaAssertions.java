package ca.gc.aafc.dina.testsupport.utils;

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

import lombok.extern.log4j.Log4j2;

@Log4j2
public final class OpenAPI3SchemaAssertions {

  public OpenAPI3SchemaAssertions() {
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

  public static String validateResponseAgainstSchema(JsonNode schNode1, String prop,
      JsonNode dataNode1) {
    String validStr = "";
    SchemaValidator schemaValidator = null;
    try {
      schemaValidator = new SchemaValidator(prop, schNode1);
    } catch (ResolutionException e) {
      return "Validation error in schema \n" + schNode1.toPrettyString();
    }
    ValidationData validationData = new ValidationData();
    boolean out1 = schemaValidator.validate(dataNode1, validationData);
    validStr = validationData.results().toString();
    return validStr;
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
    if (results != null) {
      if (results.toString().trim() != "") {
        String errors = results.toString();
        if (errors.length() > 0)
          throw new Exception(errors);
      }
    }
    return api;
  }

}
