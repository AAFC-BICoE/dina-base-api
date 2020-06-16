package ca.gc.aafc.dina.filter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import io.crnk.core.queryspec.Direction;
import io.crnk.core.queryspec.QuerySpec;
import lombok.NonNull;

@Component
public class DinaFilterResolver {

  @Inject
  private SimpleFilterHandler simpleFilterHandler;

  @Inject
  private RsqlFilterHandler rsqlFilterHandler;

  public <E> Predicate[] buildPredicates(
    @NonNull QuerySpec querySpec,
    @NonNull CriteriaBuilder cb,
    @NonNull Root<E> root,
    Collection<Serializable> ids,
    String idFieldName
  ) {
    List<javax.persistence.criteria.Predicate> restrictions = new ArrayList<>();
    restrictions.add(simpleFilterHandler.getRestriction(querySpec, root, cb));
    restrictions.add(rsqlFilterHandler.getRestriction(querySpec, root, cb));

    if (CollectionUtils.isNotEmpty(ids)) {
      restrictions.add(root.get(idFieldName).in(ids));
    }

    return restrictions.stream().toArray(javax.persistence.criteria.Predicate[]::new);
  }

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
