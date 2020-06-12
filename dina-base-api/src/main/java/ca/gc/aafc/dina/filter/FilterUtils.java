package ca.gc.aafc.dina.filter;

import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Path;

import io.crnk.core.queryspec.Direction;
import io.crnk.core.queryspec.QuerySpec;

/**
 * Utility class for crnk sorting.
 */
public final class FilterUtils {

  private FilterUtils() { }

  /**
   * Parses a crnk {@link QuerySpec} to return a list of {@link Order} from a
   * given {@link CriteriaBuilder} and {@link Path}.
   * 
   * @param <T>
   *               - root type
   * @param qs
   *               - crnk query spec to parse
   * @param cb
   *               - critera builder to build orders
   * @param root
   *               - root path of entity
   * @return
   */
  public static <T> List<Order> getOrders(QuerySpec qs, CriteriaBuilder cb, Path<T> root) {
    return qs.getSort().stream().map(sort -> {
      Path<T> from = root;
      for (String path : sort.getAttributePath()) {
        from = from.get(path);
      }
      return sort.getDirection() == Direction.ASC ? cb.asc(from) : cb.desc(from);
    }).collect(Collectors.toList());
  }

}
