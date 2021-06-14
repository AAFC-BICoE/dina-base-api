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

  public static <E extends DinaEntity> void handleOrphans(
    List<E> oldChildren,
    List<E> newChildren,
    Consumer<E> orphanConsumer
  ) {
    Map<UUID, E> oldChildrenById = oldChildren == null ? Map.of() : oldChildren.stream()
      .collect(Collectors.toMap(DinaEntity::getUuid, Function.identity()));
    Map<UUID, E> newChildrenByID = newChildren == null ? Map.of() : newChildren.stream()
      .collect(Collectors.toMap(DinaEntity::getUuid, Function.identity()));

    oldChildrenById.forEach((uuid, dinaEntity) -> {
      if (!newChildrenByID.containsKey(uuid)) {
        orphanConsumer.accept(dinaEntity);
      }
    });
  }

  public static <T> List<T> findByParent(
    Class<T> classType,
    String parentName,
    Object parentObj,
    DinaService<?> service
  ) {
    return service.findAll(classType, (criteriaBuilder, storageUnitRoot) -> new Predicate[]{
      criteriaBuilder.equal(storageUnitRoot.get(parentName), parentObj)
    }, null, 0, Integer.MAX_VALUE);
  }
}
