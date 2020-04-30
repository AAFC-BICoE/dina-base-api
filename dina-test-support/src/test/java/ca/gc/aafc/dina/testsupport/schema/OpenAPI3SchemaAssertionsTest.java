package ca.gc.aafc.dina.testsupport.schema;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.openapi4j.core.model.AuthOption;
import org.openapi4j.parser.model.v3.OpenApi3;

import com.fasterxml.jackson.databind.JsonNode;

import ca.gc.aafc.dina.testsupport.TestResourceHelper;

public class OpenAPI3SchemaAssertionsTest {

  @Test
  public void assertJsonSchemaTest() throws IOException {
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
    
    
    String jsonStr = TestResourceHelper.readContentAsString("ma-response-example.txt");
    OpenAPI3SchemaAssertions.assertSchema(schemaNode, "data", jsonStr);
  }
}
