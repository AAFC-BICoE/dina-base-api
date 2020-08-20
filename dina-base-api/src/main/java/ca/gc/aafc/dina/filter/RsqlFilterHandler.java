package ca.gc.aafc.dina.filter;

import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Predicate;

import com.github.tennaito.rsql.jpa.JpaPredicateVisitor;
import com.github.tennaito.rsql.misc.ArgumentParser;

import org.apache.commons.lang3.StringUtils;

import ca.gc.aafc.dina.jpa.BaseDAO;
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

  private final BaseDAO baseDAO;
  private final ArgumentParser rsqlArgumentParser;
  private final RSQLParser rsqlParser = new RSQLParser();

  @Override
  public Predicate getRestriction(QuerySpec querySpec, From<?, ?> root, CriteriaBuilder cb) {
    FilterSpec rsqlFilterSpec = querySpec.findFilter(PathSpec.of("rsql")).orElse(null);
    if (rsqlFilterSpec == null || StringUtils.isBlank(rsqlFilterSpec.getValue().toString())) {
      // Return a blank predicate if there is no requested RSQL filter.
      return cb.and();
    }
    
    String rsqlString = rsqlFilterSpec.getValue();

    // Add the Injected ArgumentParser into the RSQL visitor:
    JpaPredicateVisitor<?> rsqlVisitor = new JpaPredicateVisitor<>().defineRoot(root);
    rsqlVisitor.getBuilderTools().setArgumentParser(rsqlArgumentParser);

    return baseDAO.createWithEntityManager(
      em -> rsqlParser.parse(rsqlString).accept(rsqlVisitor, em));
  }

}
