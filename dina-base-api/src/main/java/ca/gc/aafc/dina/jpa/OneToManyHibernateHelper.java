package ca.gc.aafc.dina.jpa;

import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.service.DinaService;
import org.apache.commons.collections.CollectionUtils;

import javax.persistence.criteria.Predicate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

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
  public static <E extends DinaEntity> void handleOrphans(
    List<E> currentChildren,
    List<E> incomingChildren,
    Consumer<E> orphanHandler
  ) {
    Map<UUID, E> oldChildrenById = currentChildren == null ? Map.of() : currentChildren.stream()
      .collect(Collectors.toMap(DinaEntity::getUuid, Function.identity()));
    Map<UUID, E> newChildrenByID = incomingChildren == null ? Map.of() : incomingChildren.stream()
      .collect(Collectors.toMap(DinaEntity::getUuid, Function.identity()));

    oldChildrenById.forEach((uuid, dinaEntity) -> {
      if (!newChildrenByID.containsKey(uuid)) {
        orphanHandler.accept(dinaEntity);
      }
    });
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
