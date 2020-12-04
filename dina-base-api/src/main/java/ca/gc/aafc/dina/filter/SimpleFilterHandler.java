package ca.gc.aafc.dina.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Predicate;

import com.github.tennaito.rsql.misc.ArgumentParser;

import ca.gc.aafc.dina.repository.SelectionHandler;
import io.crnk.core.queryspec.FilterOperator;
import io.crnk.core.queryspec.FilterSpec;
import io.crnk.core.queryspec.QuerySpec;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Simple filter handler for filtering by a value in a single attribute.
 * Example GET request where pcrPrimer's [name] == '101F' :
 *   http://localhost:8080/api/pcrPrimer?filter[name]=101F
 */
@Named
//CHECKSTYLE:OFF AnnotationUseStyle
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SimpleFilterHandler implements FilterHandler {

  private final ArgumentParser argumentParser;

  @Override
  public Predicate getRestriction(QuerySpec querySpec, From<?, ?> root, CriteriaBuilder cb) {
    List<FilterSpec> filterSpecs = querySpec.getFilters();
    List<Predicate> predicates = new ArrayList<>();

    for (FilterSpec filterSpec : filterSpecs) {
      Expression<?> attributePath;
      try {
        attributePath = SelectionHandler.getExpression(root, filterSpec.getAttributePath());
      } catch (IllegalArgumentException e) {
        // This FilterHandler will ignore filter parameters that do not map to fields on the DTO,
        // like "rsql" or others that are only handled by other FilterHandlers.
        continue;
      }
      
      predicates.add(generatePredicate(filterSpec, attributePath, cb));
    }

    return cb.and(predicates.stream().toArray(Predicate[]::new));
  }

  /**
   * Generates a predicate for a given crnk filter spec for a given attribute
   * path. Predicate is built with the given criteria builder.
   *
   * @param filter
   *                        - filter to parse
   * @param attributePath
   *                        - path to the attribute
   * @param cb
   *                        - criteria builder to build the predicate
   * @return a predicate for a given crnk filter spec
   */
  private Predicate generatePredicate(
    @NonNull FilterSpec filter,
    @NonNull Expression<?> attributePath,
    @NonNull CriteriaBuilder cb
  ) {
    // Convert the value to the target type:
    Object value = argumentParser.parse(
      Optional.ofNullable(filter.getValue()).map(Object::toString).orElse(null),
      attributePath.getJavaType()
    );
    if (value == null) {
      return filter.getOperator() == FilterOperator.NEQ 
        ? cb.isNotNull(attributePath)
        : cb.isNull(attributePath);
    } else {
      return cb.equal(attributePath, value);
    }
  }

}