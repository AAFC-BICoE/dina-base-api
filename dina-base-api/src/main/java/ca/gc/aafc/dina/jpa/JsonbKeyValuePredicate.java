package ca.gc.aafc.dina.jpa;

import java.util.Map;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Class that can build JPA Predicates from jsonb field using PostgreSQL
 * specific jsonb_path_exists function. Since jsonb_path_exists requires 
 * parameter of type jsonpath and it is not well handled by JPA we are relying on 
 * a custom proxy function jsonb_path_exists_varchar that will be responsible to cast the varchar to jsonpath.
 * 
 * Note that jsonb_path_exists cannot use Index so it doesn't scale well.
 */
public final class JsonbKeyValuePredicate {

  // Custom function that is responsible to cast the parameters
  private static final String JSONB_EXTRACT_PATH_PG_FUNCTION_NAME = "jsonb_path_exists_varchar";
  private static final ObjectMapper OM = new ObjectMapper();

  /**
   * Generate a predicate builder based on a column name and key name. Once generated you can use
   * the builder multiple times to generate predicates.
   * 
   * @param colName JSONb column name.
   * @param keyName JSONb key name.
   * @return JsonbKeyValuePredicateBuilder
   */
  public static JsonbKeyValuePredicateBuilder onKey(String colName, String keyName) {
    return new JsonbKeyValuePredicateBuilder(colName, keyName);
  }

  public static class JsonbKeyValuePredicateBuilder {

    private final String columnName;
    private final String path;

    public JsonbKeyValuePredicateBuilder(String colName, String keyName) {
      this.columnName = colName;
      this.path = "$[*]." + keyName + " ? (@ == $val)";
    }

    public Predicate buildUsing(Path<?> root, CriteriaBuilder builder, String value, boolean caseSensitive) throws JsonProcessingException {
      return builder.isTrue(builder.function(
        JSONB_EXTRACT_PATH_PG_FUNCTION_NAME, 
        Boolean.class, 
        root.get(this.columnName),
        builder.literal(this.path),
        builder.literal(OM.writeValueAsString(Map.of("val", value))),
        builder.literal(caseSensitive)
      ));
    }
  }
}
