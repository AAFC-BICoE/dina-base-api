package ca.gc.aafc.dina.json;

import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TestConstants {
  static final TypeReference<Map<String, Object>> IT_OM_TYPE_REF = new TypeReference<>() { };
  public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
}
