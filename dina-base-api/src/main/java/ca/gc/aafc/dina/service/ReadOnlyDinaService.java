package ca.gc.aafc.dina.service;

import java.util.List;
import java.util.function.Predicate;

/**
 * Service layer to provide read-only access using Predicate.
 */
public interface ReadOnlyDinaService<T> {

  List<T> findAll(Predicate<T> predicate, Integer pageOffset, Integer pageLimit);

}
