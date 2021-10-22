package ca.gc.aafc.dina.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

@Getter
@Setter
@RequiredArgsConstructor
public class JsonbValueSpecification<T> {

  private final String columnName;
  private final String keyName;
  private final String value;

  public Predicate toPredicate(Root<T> root, CriteriaBuilder builder) {
    return builder.equal(
      builder.function(
        "jsonb_extract_path_text",
        String.class,
        root.get(this.columnName),
        builder.literal(this.keyName)),
      this.value);
  }
}
