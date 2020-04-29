package ca.gc.aafc.dina.testsupport.utils;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.util.Strings;
import org.junit.Test;
import org.openapi4j.core.model.AuthOption;
import org.openapi4j.parser.model.v3.OpenApi3;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class OpenAPI3SchemaAssertionsTest {

  @Test
  public void assertJsonSchemaTest() {
    URL url1 = this.getClass().getResource("/managedAttribute.yaml");
    OpenApi3 api;
    List<AuthOption> authOptions = new ArrayList<>();
    try {
      api = OpenAPI3SchemaAssertions.parseAndValidateOpenAPI3Doc(url1, authOptions);
    } catch (Exception e1) {
      fail(e1.getMessage());
      return;
    }

    JsonNode schemaNode;
    try {
      schemaNode = OpenAPI3SchemaAssertions.getJsonNodeForSchemaName("ManagedAttribute", api);
    } catch (Exception e1) {
      fail(e1.getMessage());
      return;
    }
    URL url2 = this.getClass().getResource("/ma-response-example.txt");
    BufferedReader apiResponse = null;
    try {
      apiResponse = new BufferedReader(new InputStreamReader(url2.openStream()));
    } catch (IOException e2) {
      fail(e2.getMessage());
      return;
    }

    //FIXME
/*
    String validationResult = OpenAPI3SchemaAssertions.assertSchema(schemaNode,
        "data", );
    // System.out.println(validationResult);
    assertTrue(!(validationResult.contains("Validation error")), validationResult.toString());*/
  }
}
