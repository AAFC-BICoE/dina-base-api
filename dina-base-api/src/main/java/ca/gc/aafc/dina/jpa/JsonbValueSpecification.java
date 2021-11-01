package ca.gc.aafc.dina.jpa;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import java.util.stream.Stream;

/**
 * Allows to create JPA Predicate matching a value on a specific jsonb key. PostgreSQL specific.
 *
 * @param <T>
 */
public class JsonbValueSpecification<T> {

  static final String JSONB_EXTRACT_PATH_PG_FUNCTION_NAME = "jsonb_path_exists";

  private final String columnName;
  private final String[] keys;

  public JsonbValueSpecification(String columnName, String... keys) {
    this.columnName = columnName;
    this.keys = keys;
  }

  public Predicate toPredicate(Path<T> root, CriteriaBuilder builder, String value) {
    if (ArrayUtils.isNotEmpty(keys)) {
      Expression<?>[] literals = {root.get(this.columnName)};

      String path = "$[*]." + StringUtils.join(Stream.of(keys), ".") + " ? (@ == $val)";

      Expression<Object> pathExpression = builder.function("cast", Object.class,
        builder.literal(path + " as int ")); //TODO how to cast to JSONPATH ??

      Expression<Object> valueExpression = builder.function("json_build_object", Object.class,
        builder.literal("val"),
        builder.literal(value));

      Expression<Object> to_jsonb = builder.function("to_jsonb", Object.class, valueExpression);

      literals = ArrayUtils.add(literals, builder.literal(path));
      literals = ArrayUtils.add(literals, to_jsonb);

      return builder.equal(
        builder.function(
          JSONB_EXTRACT_PATH_PG_FUNCTION_NAME,
          Boolean.class,
          literals),
        true);
    }
    return builder.and();
  }
}
