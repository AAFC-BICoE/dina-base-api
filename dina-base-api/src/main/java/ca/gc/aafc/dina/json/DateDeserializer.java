package ca.gc.aafc.dina.json;

import java.sql.Date;

import org.springframework.boot.jackson.JacksonComponent;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

/**
 * Deserializer for ObjectMapper that converts a string of YYYY-MM-DD format to a java.sql.date.
 */
@JacksonComponent
public class DateDeserializer extends ValueDeserializer<Date> {

  @Override
  public Date deserialize(JsonParser parser, DeserializationContext ctxt) {
    String text = parser.getString();
    try {
      return Date.valueOf(text);
    } catch (IllegalArgumentException e) {
      // the goal is to set a more specific message
      throw new IllegalArgumentException(String.format("\"%s\": The date given is not in the JDBC date escape format (yyyy-[m]m-[d]d).", text));
    }
  }
}
