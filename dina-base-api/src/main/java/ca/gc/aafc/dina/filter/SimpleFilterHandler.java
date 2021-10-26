package ca.gc.aafc.dina.filter;

import ca.gc.aafc.dina.repository.SelectionHandler;
import com.github.tennaito.rsql.misc.ArgumentParser;
import io.crnk.core.queryspec.FilterOperator;
import io.crnk.core.queryspec.FilterSpec;
import io.crnk.core.queryspec.QuerySpec;
import lombok.NonNull;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Simple filter handler for filtering by a value in a single attribute. Example GET request where pcrPrimer's
 * [name] == '101F' : http://localhost:8080/api/pcrPrimer?filter[name]=101F
 */
public final class SimpleFilterHandler {

  private SimpleFilterHandler() {
  }

  /**
   * Generates a predicate for a given crnk filter.
   *
   * @param querySpec      - crnk query spec with filters, cannot be null
   * @param cb             - the criteria builder, cannot be null
   * @param root           - the root type, cannot be null
   * @param argumentParser - used to parse the arguments into there given types. See {@link
   *                       DinaFilterArgumentParser}
   * @return Generates a predicate for a given crnk filter.
   */
  public static Predicate getRestriction(
    @NonNull QuerySpec querySpec,
    @NonNull Root<?> root,
    @NonNull CriteriaBuilder cb,
    @NonNull ArgumentParser argumentParser
  ) {
    List<FilterSpec> filterSpecs = querySpec.getFilters();
    List<Predicate> predicates = new ArrayList<>();

    for (FilterSpec filterSpec : filterSpecs) {
      Expression<?> attributePath;
      try {
        attributePath = SelectionHandler.getExpression(root, filterSpec.getAttributePath());
        predicates.add(generatePredicate(filterSpec, attributePath, cb, argumentParser));
      } catch (IllegalArgumentException e) {
        // This FilterHandler will ignore filter parameters that do not map to fields on the DTO,
        // like "rsql" or others that are only handled by other FilterHandlers.
      }
    }

    return cb.and(predicates.toArray(Predicate[]::new));
  }

  /**
   * Generates a predicate for a given crnk filter spec for a given attribute path. Predicate is built with
   * the given criteria builder.
   *
   * @param filter         - filter to parse
   * @param attributePath  - path to the attribute
   * @param cb             - criteria builder to build the predicate
   * @param argumentParser - the argument parser
   * @return a predicate for a given crnk filter spec
   */
  private static Predicate generatePredicate(
    @NonNull FilterSpec filter,
    @NonNull Expression<?> attributePath,
    @NonNull CriteriaBuilder cb,
    @NonNull ArgumentParser argumentParser
  ) {
    // Convert the value to the target type:
    Object filterValue = filter.getValue();
    if (filterValue == null) {
      return filter.getOperator() == FilterOperator.NEQ
        ? cb.isNotNull(attributePath)
        : cb.isNull(attributePath);
    } else {
      Object value = argumentParser.parse(filterValue.toString(), attributePath.getJavaType());
      return cb.equal(attributePath, value);
    }
  }

}
