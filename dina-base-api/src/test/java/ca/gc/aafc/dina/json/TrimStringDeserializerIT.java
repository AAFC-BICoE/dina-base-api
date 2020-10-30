package ca.gc.aafc.dina.json;

import ca.gc.aafc.dina.TestDinaBaseApp;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.inject.Inject;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tested as integration test since we need to make sure the deserializer is used by the Spring
 * managed ObjectMapper.
 */
@SpringBootTest(classes = TestDinaBaseApp.class)
public class TrimStringDeserializerIT {

  @Inject
  private ObjectMapper objectMapper;

  /**
   * Test class for deserializing a date.
   */
  @Data
  private static class StringWrapper {
    private String value;
  }

  @Test
  public void trimJsonString_whenStringContainsLeadingTrailingWhitespaces_whitespacesAreRemoved() throws IOException {
    String jsonValue = "{\"value\": \"  this is a value" + 	"\\t"+ "\"}";
    TrimStringDeserializerIT.StringWrapper stringWrapper = objectMapper.readValue(jsonValue,
        TrimStringDeserializerIT.StringWrapper.class);
    assertEquals("this is a value", stringWrapper.getValue());
  }

  @Test
  public void trimJsonString_whenStringContainsWhitespacesInString_whitespacesAreNotRemoved() throws IOException {
    String jsonValue = "{\"value\": \"this is  a  value\"}";
    TrimStringDeserializerIT.StringWrapper stringWrapper = objectMapper.readValue(jsonValue,
        TrimStringDeserializerIT.StringWrapper.class);
    assertEquals("this is  a  value", stringWrapper.getValue());
  }
}
