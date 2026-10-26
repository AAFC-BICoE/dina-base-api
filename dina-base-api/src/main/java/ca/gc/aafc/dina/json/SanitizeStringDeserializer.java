package ca.gc.aafc.dina.json;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.jdk.StringDeserializer;
import org.springframework.boot.jackson.JacksonComponent;

/**
 * Jackson String deserializer to trim all strings to avoid having to deal with them downstream.
 * There is no know use cases where leading and trailing whitespaces should be preserved.
 */
@JacksonComponent
public class SanitizeStringDeserializer extends StringDeserializer {

  @Override
  public String deserialize(JsonParser p, DeserializationContext ctxt) {
    String value = super.deserialize(p, ctxt);
    return value != null ? value.strip() : null;
  }

}
