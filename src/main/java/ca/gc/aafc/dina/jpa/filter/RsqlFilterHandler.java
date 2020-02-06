package ca.gc.aafc.dina.jpa.filter;

import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Predicate;

import org.apache.commons.lang3.StringUtils;

import com.github.tennaito.rsql.jpa.JpaPredicateVisitor;

import cz.jirutka.rsql.parser.RSQLParser;
import io.crnk.core.queryspec.FilterSpec;
import io.crnk.core.queryspec.PathSpec;
import io.crnk.core.queryspec.QuerySpec;
import lombok.RequiredArgsConstructor;

/**
 * Filter Handler that allows complex filtering using RSQL.
 * Example query:
 *   localhost:8080/api/region?filter[rsql]=( name=='12S' or name=='142' )
 *   
 * RSQL spec: https://github.com/jirutka/rsql-parser
 */
@Named
//CHECKSTYLE:OFF AnnotationUseStyle
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class RsqlFilterHandler implements FilterHandler {

  private final EntityManager entityManager;
  
  private final RSQLParser rsqlParser = new RSQLParser();
  
  @Override
  public Predicate getRestriction(QuerySpec querySpec, From<?, ?> root, CriteriaQuery<?> query,
      CriteriaBuilder cb) {
    FilterSpec rsqlFilterSpec = querySpec.findFilter(PathSpec.of("rsql")).orElse(null);
    if (rsqlFilterSpec == null || StringUtils.isBlank(rsqlFilterSpec.getValue().toString())) {
      // Return a blank predicate if there is no requested RSQL filter.
      return cb.and();
    }
    
    String rsqlString = rsqlFilterSpec.getValue();
    
    return rsqlParser.parse(rsqlString)
        .accept(new JpaPredicateVisitor<>().defineRoot(root), entityManager);
  }

}
