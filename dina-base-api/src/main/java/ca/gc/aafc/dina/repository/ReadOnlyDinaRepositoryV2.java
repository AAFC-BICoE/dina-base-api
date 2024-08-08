package ca.gc.aafc.dina.repository;

import java.util.List;
import java.util.function.Predicate;

import ca.gc.aafc.dina.filter.FilterComponent;
import ca.gc.aafc.dina.filter.QueryComponent;
import ca.gc.aafc.dina.filter.QueryStringParser;
import ca.gc.aafc.dina.filter.SimpleObjectFilterHandlerV2;
import ca.gc.aafc.dina.service.PredicateBasedReadOnlyDinaService;

/**
 * Based repository for accessing read-only data.
 */
public class ReadOnlyDinaRepositoryV2<K,D> {

  private final PredicateBasedReadOnlyDinaService<K,D> service;

  public ReadOnlyDinaRepositoryV2(PredicateBasedReadOnlyDinaService<K,D> service) {
    this.service = service;
  }

  public D findOne(K key) {
    return service.findOne(key);
  }

  public List<D> findAll(String queryString) {
    QueryComponent queryComponents = QueryStringParser.parse(queryString);

    FilterComponent fc = queryComponents.getFilters();

    Predicate<D> predicate = SimpleObjectFilterHandlerV2.createPredicate(fc);
    return service.findAll(predicate, queryComponents.getPageOffset(), queryComponents.getPageLimit());
  }
}
