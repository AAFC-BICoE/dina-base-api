package ca.gc.aafc.dina.service;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implementation of {@link PredicateBasedReadOnlyDinaService} that is backed by a Java collection.
 *
 * @param <K>
 * @param <R>
 */
public abstract class CollectionBackedReadOnlyDinaService<K, R> implements PredicateBasedReadOnlyDinaService<K, R> {

  private final Collection<R> collection;
  private final Function<R, K> getKeyFunction;

  public CollectionBackedReadOnlyDinaService(Collection<R> collection,
                                             Function<R, K> getKeyFunction) {
    this.collection = collection;
    this.getKeyFunction = getKeyFunction;
  }

  @Override
  public R findOne(K key) {
    return collection.stream()
      .filter(r -> getKeyFunction.apply(r).equals(key)).findFirst()
      .orElse(null);
  }

  @Override
  public List<R> findAll(Predicate<R> predicate, Comparator<R> sortComparator, Integer pageOffset, Integer pageLimit) {
    Stream<R> stream = collection.stream();
    if (predicate != null) {
      stream = stream.filter(predicate);
    }

    if(sortComparator != null) {
      stream = stream.sorted(sortComparator);
    }

    if (pageOffset != null) {
      stream = stream.skip(pageOffset);
    }
    if (pageLimit != null) {
      stream = stream.limit(pageLimit);
    }
    return stream.collect(Collectors.toList());
  }
}
