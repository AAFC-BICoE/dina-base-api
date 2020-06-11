package ca.gc.aafc.dina.json;

import java.io.IOException;
import java.sql.Date;

import org.springframework.boot.jackson.JsonComponent;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import io.crnk.core.engine.document.ErrorData;
import io.crnk.core.engine.http.HttpStatus;
import io.crnk.core.exception.CrnkMappableException;

/**
 * Deserializer for ObjectMapper that converts a string of YYYY-MM-DD format to a java.sql.date.
 */
@JsonComponent
public class DateDeserializer extends JsonDeserializer<Date> {

  private static final Integer STATUS_ON_ERROR = HttpStatus.UNPROCESSABLE_ENTITY_422;
  
  @Override
  public Date deserialize(JsonParser parser, DeserializationContext ctxt)
      throws IOException, JsonProcessingException {
    String text = parser.getText();
    try {
      return Date.valueOf(text);
    } catch (IllegalArgumentException e) {
      ErrorData errorData = ErrorData.builder()
          .setStatus(STATUS_ON_ERROR.toString())
          .setTitle("Date format error")
          .setDetail(String.format("\"%s\": The date given is not in the JDBC date escape format (yyyy-[m]m-[d]d).", text))
          .build();
      
      throw new CrnkMappableException(STATUS_ON_ERROR, errorData, e) {
        private static final long serialVersionUID = 8177279204226674938L;
      };
    }
  }

}
