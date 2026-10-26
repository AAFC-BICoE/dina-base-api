package ca.gc.aafc.dina.client.token;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.json.JsonMapper;

/**
 * Minimal Retrofit response converter based on Jackson 3 (Retrofit only ships a Jackson 2 converter).
 */
public final class Jackson3ConverterFactory extends Converter.Factory {

  private final JsonMapper mapper;

  private Jackson3ConverterFactory(JsonMapper mapper) {
    this.mapper = mapper;
  }

  public static Jackson3ConverterFactory create() {
    return new Jackson3ConverterFactory(JsonMapper.builder().build());
  }

  @Override
  public Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations,
                                                          Retrofit retrofit) {
    JavaType javaType = mapper.getTypeFactory().constructType(type);
    ObjectReader reader = mapper.readerFor(javaType);
    return body -> {
      try (body) {
        return reader.readValue(body.charStream());
      }
    };
  }
}
