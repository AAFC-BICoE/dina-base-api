package ca.gc.aafc.dina.testsupport.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.openapi4j.core.exception.EncodeException;
import org.openapi4j.core.exception.ResolutionException;
import org.openapi4j.schema.validator.ValidationData;
import org.openapi4j.schema.validator.v3.SchemaValidator;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;

import lombok.extern.log4j.Log4j2;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.openapi4j.core.model.AuthOption;
import org.openapi4j.core.validation.ValidationException;
import org.openapi4j.core.validation.ValidationResults;
import org.openapi4j.parser.OpenApi3Parser;
import org.openapi4j.parser.model.v3.OpenApi3;
import org.openapi4j.parser.model.v3.Schema;
import org.openapi4j.parser.validation.v3.OpenApi3Validator;

@Log4j2
public final class OpenAPI3SchemaAssertions {

  private OpenAPI3SchemaAssertions() {
  }

  /**
   * Assert that the provided apiResponse validates against the provided OpenAPI schema.
   * 
   * @param uri
   *          the uri for the OpenAPI schema
   * @param apiResponse
   *          the api response
   * @throws IOException
   */
  public static void assertJsonSchema(URI uri, Reader apiResponse) throws IOException {
    URL url1 = uri.toURL();
    JsonNode schemaNode = JsonLoader.fromURL(url1);
    JsonNode contentNode = JsonLoader.fromReader(apiResponse);
    SchemaValidator schemaValidator = null;
    try {
      schemaValidator = new SchemaValidator("Managed attributes schema", schemaNode);
    } catch (ResolutionException e) {
      fail("Trying to assert schema located at " + url1.toString() + ": "
          + "Expected resource could not be reached");
    }
    ValidationData validationData = new ValidationData();
    boolean out1 = schemaValidator.validate(contentNode, validationData);
    assertTrue(out1, validationData.results().toString());
  }

  private static JsonNode getJsonNodeForSchemaName(String schemaName, OpenApi3 api1) {
    JsonNode schemaNode = null;
    Schema schema1 = api1.getComponents().getSchema(schemaName);
    try {
      schemaNode = schema1.toNode(api1.getContext(), true);
    } catch (EncodeException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }
    return schemaNode;
  }
  
  public static void main(String[] args){
    OpenAPI3SchemaAssertions assertion = new OpenAPI3SchemaAssertions();
    URL url1 = assertion.getClass().getResource("/managedAttribute.yaml");
    OpenApi3 api;
    List<AuthOption> authOptions = new ArrayList<>();
    try {
      api = parseAndValidateOpenAPI3Doc(url1, authOptions);
    } catch (Exception e1) {
      System.out.println(e1.getMessage());
      return;
    }
    JsonNode schemaNode = getJsonNodeForSchemaName("ManagedAttribute", api);
    //System.out.println(schemaNode.toPrettyString());
    URL url2 = assertion.getClass().getResource("/ma-response-example.txt");
    BufferedReader apiResponse = null;
    try {
      apiResponse = new BufferedReader(new InputStreamReader(url2.openStream()));
    } catch (IOException e2) {
      // TODO Auto-generated catch block
      e2.printStackTrace();
    }
    JsonNode contentNode = null;
    try {
      contentNode = JsonLoader.fromReader(apiResponse);
    } catch (IOException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }
    String validationResult = validateResponseAgainstSchema(schemaNode, "data", contentNode);
    //System.out.println(validationResult);
    assertTrue(!(validationResult.contains("Validation error")), validationResult.toString());
  }

  private static String validateResponseAgainstSchema(JsonNode schNode1, String prop, JsonNode dataNode1) {
    String validStr = "";
    SchemaValidator schemaValidator = null;
    try {
      schemaValidator = new SchemaValidator(prop, schNode1);
    } catch (ResolutionException e) {
      return "Error in validating schema \n" + schNode1.toPrettyString();
    }
    ValidationData validationData = new ValidationData();
    boolean out1 = schemaValidator.validate(dataNode1, validationData); 
    validStr = validationData.results().toString();
    return validStr;
  }
  
  private static OpenApi3 parseAndValidateOpenAPI3Doc(URL url1, List<AuthOption> authOptions)
      throws Exception {
    OpenApi3 api = null;
    try {
      api = new OpenApi3Parser().parse(url1, authOptions, false);
    } catch (ResolutionException | ValidationException e) {
      if (e instanceof ResolutionException) {
        throw new Exception("Problem in reaching out to OpenAPI Document - " + e.getMessage());
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
