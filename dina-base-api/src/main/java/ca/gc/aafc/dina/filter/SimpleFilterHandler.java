package ca.gc.aafc.dina.filter;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Predicate;

import ca.gc.aafc.dina.repository.SelectionHandler;
import io.crnk.core.queryspec.FilterSpec;
import io.crnk.core.queryspec.QuerySpec;
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

  @Override
  public Predicate getRestriction(
      QuerySpec querySpec,
      From<?, ?> root,
      CriteriaQuery<?> query,
      CriteriaBuilder cb
  ) {
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
      predicates.add(cb.equal(attributePath, (Object) filterSpec.getValue()));
    }

    return cb.and(predicates.stream().toArray(Predicate[]::new));
  }

}
