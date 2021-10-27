package ca.gc.aafc.dina.jpa;

import org.apache.commons.lang3.ArrayUtils;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;

/**
 * Allows to create JPA Predicate matching a value on a specific jsonb key. PostgreSQL specific.
 *
 * @param <T>
 */
public class JsonbValueSpecification<T> {

  static final String JSONB_EXTRACT_PATH_PG_FUNCTION_NAME = "jsonb_extract_path_text";

  private final String columnName;
  private final String[] keys;

  public JsonbValueSpecification(String columnName, String... keys) {
    this.columnName = columnName;
    this.keys = keys;
  }

  public Predicate toPredicate(Path<T> root, CriteriaBuilder builder, String value) {
    if (ArrayUtils.isNotEmpty(keys)) {
      Expression<?>[] literals = {root.get(this.columnName)};
      for (String key : keys) {
        literals = ArrayUtils.add(literals, builder.literal(key));
      }
      return builder.equal(
        builder.function(
          JSONB_EXTRACT_PATH_PG_FUNCTION_NAME,
          String.class,
          literals),
        value);
    }
    return builder.and();
  }
}
