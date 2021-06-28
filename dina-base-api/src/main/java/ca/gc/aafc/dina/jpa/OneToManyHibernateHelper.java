package ca.gc.aafc.dina.jpa;

import ca.gc.aafc.dina.service.DinaService;
import org.apache.commons.collections.CollectionUtils;

import javax.persistence.criteria.Predicate;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Helper class to resolve one to many relations for Parent resources.
 */
public final class OneToManyHibernateHelper {

  private OneToManyHibernateHelper() {
  }

  /**
   * Helper method to apply the given parent apply method to a list of children.
   *
   * @param children          children to apply parent too
   * @param parent            parent to apply
   * @param parentApplyMethod method to apply the parent to the children
   * @param <C>               Child type
   * @param <P>               Parent type
   */
  public static <C, P> void linkChildren(
    List<C> children,
    P parent,
    Function<C, Consumer<P>> parentApplyMethod
  ) {
    if (CollectionUtils.isNotEmpty(children)) {
      children.forEach(c -> parentApplyMethod.apply(c).accept(parent));
    }
  }

  /**
   * Helper method to handle orphaned resources. Runs the orphan handler on the given list of currentChildren
   * that are not present in the given incomingChildren.
   *
   * @param currentChildren  current children to evaluate
   * @param incomingChildren incoming children to evaluate
   * @param orphanHandler    consumer to handle the evaluated orphans
   * @param <E>              Child type
   */
  public static <E> void handleOrphans(
    List<E> currentChildren,
    List<E> incomingChildren,
    BiFunction<E, E, Boolean> childEqualityMethod,
    Consumer<E> orphanHandler
  ) {
    if (CollectionUtils.isEmpty(currentChildren) || childEqualityMethod == null || orphanHandler == null) {
      return;
    }

    if (CollectionUtils.isEmpty(incomingChildren)) {
      currentChildren.forEach(orphanHandler);
    } else {
      currentChildren.forEach(current -> {
        if (incomingChildren.stream().noneMatch(incoming -> childEqualityMethod.apply(current, incoming))) {
          orphanHandler.accept(current);
        }
      });
    }
  }

  /**
   * Finds a List of resources with a given parent object.
   *
   * @param classType  type of resource
   * @param parentFieldName name of the parent field
   * @param parentObj  parent object of the resource
   * @param service    database service to use
   * @param <T>        resource type
   * @return List of resources with a given parent object.
   */
  public static <T> List<T> findByParent(
    Class<T> classType,
    String parentFieldName,
    Object parentObj,
    DinaService<?> service
  ) {
    return service.findAll(classType, (criteriaBuilder, storageUnitRoot) -> new Predicate[]{
      criteriaBuilder.equal(storageUnitRoot.get(parentFieldName), parentObj)
    }, null, 0, Integer.MAX_VALUE);
  }
}
