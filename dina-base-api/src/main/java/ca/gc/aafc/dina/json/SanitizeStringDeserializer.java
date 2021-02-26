package ca.gc.aafc.dina.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StringDeserializer;
import org.springframework.boot.jackson.JsonComponent;

import java.io.IOException;

/**
 * Jackson String deserializer to trim all strings to avoid having to deal with them downstream.
 * There is no know use cases where leading and trailing whitespaces should be preserved.
 */
@JsonComponent
public class SanitizeStringDeserializer extends StringDeserializer {

  @Override
  public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    String value = super.deserialize(p, ctxt);
    return value != null ? value.strip() : null;
  }

}
