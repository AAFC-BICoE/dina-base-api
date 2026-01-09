package ca.gc.aafc.dina.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.sql.Date;

import javax.inject.Inject;

import ca.gc.aafc.dina.BasePostgresItContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import lombok.Data;

public class DateDeserializerIT extends BasePostgresItContext {

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
  public void parseDate_whenDateIsValid_dateIsDeserialized() throws IOException {
    String jsonDateWrapper = "{\"date\": \"2019-03-27\"}";
    DateWrapper dateWrapper = objectMapper.readValue(jsonDateWrapper, DateWrapper.class);
    assertEquals("2019-03-27", dateWrapper.getDate().toString());
  }
  
  @Test
  public void parseDate_whenDateIsInvalid_ExceptionIsThrown() throws IOException {
    String jsonDateWrapper = "{\"date\": \"bad value\"}";
    try {
      objectMapper.readValue(jsonDateWrapper, DateWrapper.class);
      fail("Exception not thrown.");
    } catch(JsonMappingException exception) {
      IllegalArgumentException cause = (IllegalArgumentException) exception.getCause();
      assertEquals("\"bad value\": The date given is not in the JDBC date escape format (yyyy-[m]m-[d]d).", cause.getMessage());
    }
  }
}
