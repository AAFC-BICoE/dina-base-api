package ca.gc.aafc.dina.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.sql.Date;

import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import ca.gc.aafc.dina.TestConfiguration;
import io.crnk.core.engine.document.ErrorData;
import io.crnk.core.exception.CrnkMappableException;
import lombok.Data;

@SpringBootTest(classes = TestConfiguration.class)
public class DateDeserializerIT {

  @Inject
  private ObjectMapper objectMapper;
  
  /**
   * Test class for deserializing a date.
   */
  @Data
  private static class DateWrapper {
    private Date date;
  }
  
  @Test
  public void parseDate_whenDateIsValid_dateIsDeserialized() throws JsonParseException, JsonMappingException, IOException {
    String jsonDateWrapper = "{\"date\": \"2019-03-27\"}";
    DateWrapper dateWrapper = objectMapper.readValue(jsonDateWrapper, DateWrapper.class);
    assertEquals("2019-03-27", dateWrapper.getDate().toString());
  }
  
  @Test
  public void parseDate_whenDateIsInvalid_ExceptionIsThrown() throws JsonParseException, JsonMappingException, IOException {
    String jsonDateWrapper = "{\"date\": \"bad value\"}";
    try {
      objectMapper.readValue(jsonDateWrapper, DateWrapper.class);
      fail("Exception not thrown.");
    } catch(JsonMappingException exception) {
      CrnkMappableException cause = (CrnkMappableException) exception.getCause();
      assertEquals(422, cause.getHttpStatus());
      assertEquals("\"bad value\": The date given is not in the JDBC date escape format (yyyy-[m]m-[d]d).", cause.getMessage());
      
      ErrorData error = cause.getErrorData();
      
      // Assert correct error message, status and title
      assertEquals("\"bad value\": The date given is not in the JDBC date escape format (yyyy-[m]m-[d]d).", error.getDetail());
      assertEquals("422", error.getStatus());
      assertEquals("Date format error", error.getTitle());
    }
  }
  
}
