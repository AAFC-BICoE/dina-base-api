package ca.gc.aafc.dina.jpa;

import lombok.RequiredArgsConstructor;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;

/**
 * Allows to create JPA Predicate matching a value on a specific jsonb key.
 * PostgreSQL specific.
 * @param <T>
 */
@RequiredArgsConstructor
public class JsonbValueSpecification<T> {

  static final String JSONB_EXTRACT_PATH_PG_FUNCTION_NAME = "jsonb_extract_path_text";

  private final String columnName;
  private final String keyName;

  public Predicate toPredicate(Path<T> root, CriteriaBuilder builder, String value) {
    return builder.equal(builder
        .function(JSONB_EXTRACT_PATH_PG_FUNCTION_NAME,
            String.class,
            root.get(this.columnName),
            builder.literal(this.keyName)),
        value);
  }
}
