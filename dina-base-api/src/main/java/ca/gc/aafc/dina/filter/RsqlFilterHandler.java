package ca.gc.aafc.dina.filter;

import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.criteria.Predicate;

import com.github.tennaito.rsql.jpa.JpaPredicateVisitor;
import com.github.tennaito.rsql.misc.ArgumentParser;

import cz.jirutka.rsql.parser.ast.Node;
import org.apache.commons.collections.CollectionUtils;

import ca.gc.aafc.dina.jpa.BaseDAO;
import cz.jirutka.rsql.parser.RSQLParser;
import lombok.RequiredArgsConstructor;

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
public class RsqlFilterHandler {

  private final BaseDAO baseDAO;
  private final ArgumentParser rsqlArgumentParser;
  private final RSQLParser rsqlParser = new RSQLParser();

  public Predicate getRestriction(
    Set<RsqlFilterAdapter> adapters,
    String rsqlString, JpaPredicateVisitor<Object> visitor
  ) {
    // Add the Injected ArgumentParser into the RSQL visitor:
    visitor.getBuilderTools().setArgumentParser(rsqlArgumentParser);

    final Node rsqlNode = processAdapters(rsqlString, adapters);

    return baseDAO.createWithEntityManager(em -> rsqlNode.accept(visitor, em));
  }

  private Node processAdapters(String rsqlString, Set<RsqlFilterAdapter> adapters) {
    Node rsqlNode = rsqlParser.parse(rsqlString);
    if (CollectionUtils.isNotEmpty(adapters)) {
      for (RsqlFilterAdapter adapter : adapters) {
        rsqlNode = adapter.process(rsqlNode);
      }
    }
    return rsqlNode;
  }

}
