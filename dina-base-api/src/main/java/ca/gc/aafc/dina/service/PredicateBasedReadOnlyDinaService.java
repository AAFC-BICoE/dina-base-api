package ca.gc.aafc.dina.service;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/**
 * Service layer to provide read-only access using {@link Predicate}.
 */
public interface PredicateBasedReadOnlyDinaService<K,T> {

  T findOne(K key);

  /**
   * @param predicate  predicate or null
   * @param sortComparator sort comparator or null
   * @param pageOffset offset or null to use default
   * @param pageLimit  limit or null to use default
   * @return
   */
  List<T> findAll(Predicate<T> predicate, Comparator<T> sortComparator, Integer pageOffset,
                  Integer pageLimit);

}
