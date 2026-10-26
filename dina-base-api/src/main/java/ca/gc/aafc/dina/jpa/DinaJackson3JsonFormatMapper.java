package ca.gc.aafc.dina.jpa;

import java.lang.reflect.Type;

import org.hibernate.type.format.AbstractJsonFormatMapper;

import tools.jackson.databind.JavaType;
import tools.jackson.databind.json.JsonMapper;

/**
 * Hibernate JSON {@link org.hibernate.type.format.FormatMapper} based on Jackson 3.
 * Hibernate only ships a Jackson 2 implementation.
 * 
 * Provided by Hibernate starting at 7.3
 */
public class DinaJackson3JsonFormatMapper extends AbstractJsonFormatMapper {

  private final JsonMapper mapper;

  public DinaJackson3JsonFormatMapper() {
    this(JsonMapper.builder().findAndAddModules().build());
  }

  public DinaJackson3JsonFormatMapper(JsonMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  protected <T> T fromString(CharSequence charSequence, Type type) {
    JavaType javaType = mapper.constructType(type);
    return mapper.readValue(charSequence.toString(), javaType);
  }

  @Override
  protected <T> String toString(T value, Type type) {
    return mapper.writerFor(mapper.constructType(type)).writeValueAsString(value);
  }
}
