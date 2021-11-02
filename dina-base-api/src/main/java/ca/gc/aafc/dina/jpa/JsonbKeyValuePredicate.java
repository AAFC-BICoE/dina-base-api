package ca.gc.aafc.dina.jpa;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import java.util.Map;

public class JsonbKeyValuePredicate<T> {

  // Custom function that is responsible to cast the parameters
  static final String JSONB_EXTRACT_PATH_PG_FUNCTION_NAME = "jsonb_path_exists_varchar";

  private final String columnName;
  private static final ObjectMapper OM = new ObjectMapper();
  private final String path;

  public JsonbKeyValuePredicate(String columnName, String keyName) {
    this.columnName = columnName;
    this.path = "$[*]." + keyName + " ? (@ == $val)";
  }

  public Predicate toPredicate(Path<T> root, CriteriaBuilder builder, String value) throws JsonProcessingException {
    return builder.isTrue(builder
      .function(
        JSONB_EXTRACT_PATH_PG_FUNCTION_NAME,
        Boolean.class,
        root.get(this.columnName),
        builder.literal(path),
        builder.literal(OM.writeValueAsString(Map.of("val", value)))));
  }
}
