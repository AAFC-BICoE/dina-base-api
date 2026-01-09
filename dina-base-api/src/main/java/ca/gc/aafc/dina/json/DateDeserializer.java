package ca.gc.aafc.dina.json;

import java.io.IOException;
import java.sql.Date;

import org.springframework.boot.jackson.JsonComponent;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * Deserializer for ObjectMapper that converts a string of YYYY-MM-DD format to a java.sql.date.
 */
@JsonComponent
public class DateDeserializer extends JsonDeserializer<Date> {

  @Override
  public Date deserialize(JsonParser parser, DeserializationContext ctxt)
      throws IOException {
    String text = parser.getText();
    try {
      return Date.valueOf(text);
    } catch (IllegalArgumentException e) {
      // the goal is to set a more specific message
      throw new IllegalArgumentException(String.format("\"%s\": The date given is not in the JDBC date escape format (yyyy-[m]m-[d]d).", text));
    }
  }
}
