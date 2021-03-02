package ca.gc.aafc.dina.filter;

import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Predicate;

import com.github.tennaito.rsql.jpa.JpaPredicateVisitor;
import com.github.tennaito.rsql.misc.ArgumentParser;

import cz.jirutka.rsql.parser.ast.Node;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import ca.gc.aafc.dina.jpa.BaseDAO;
import cz.jirutka.rsql.parser.RSQLParser;
import io.crnk.core.queryspec.FilterSpec;
import io.crnk.core.queryspec.PathSpec;
import io.crnk.core.queryspec.QuerySpec;
import lombok.RequiredArgsConstructor;

import java.util.HashSet;
import java.util.Set;

/**
 * Filter Handler that allows complex filtering using RSQL. Example query: localhost:8080/api/region?filter[rsql]=(
 * name=='12S' or name=='142' )
 * <p>
 * RSQL spec: https://github.com/jirutka/rsql-parser
 */
@Named
//CHECKSTYLE:OFF AnnotationUseStyle
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class RsqlFilterHandler implements FilterHandler {

  private final BaseDAO baseDAO;
  private final ArgumentParser rsqlArgumentParser;
  private final RSQLParser rsqlParser = new RSQLParser();
  private final Set<RsqlFilterAdapter> adapters = new HashSet<>();

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

    final Node rsqlNode = processAdapters(rsqlString);

    return baseDAO.createWithEntityManager(em -> rsqlNode.accept(rsqlVisitor, em));
  }

  private Node processAdapters(String rsqlString) {
    Node rsqlNode = rsqlParser.parse(rsqlString);
    for (RsqlFilterAdapter adapter : adapters) {
      rsqlNode = adapter.process(rsqlNode);
    }
    return rsqlNode;
  }

  /**
   * clear or set the adapters {@link ca.gc.aafc.dina.filter.RsqlFilterAdapter}'s
   *
   * @param adapters - Adapters to set, can be null or empty to clear adapters.
   */
  public void setAdapters(Set<RsqlFilterAdapter> adapters) {
    this.adapters.clear();
    if (CollectionUtils.isNotEmpty(adapters)) {
      this.adapters.addAll(adapters);
    }
  }
}
