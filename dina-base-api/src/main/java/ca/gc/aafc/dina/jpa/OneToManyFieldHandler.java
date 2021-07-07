package ca.gc.aafc.dina.jpa;

import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.service.DinaService;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * OneToManyFieldHandlers are used to resolve parent and child associations for One to Many relations during
 * common create/update/delete operations.
 *
 * @param <C> Child type
 * @param <P> Parent type
 */
@RequiredArgsConstructor
public class OneToManyFieldHandler<P, C extends DinaEntity> {

  /* Class type of the child resource */
  private final Class<C> childClassType;

  /* Method to apply the parent to the child */
  private final Function<C, Consumer<P>> parentApplyMethod;

  /* Method to supply the children from the parent */
  private final Function<P, List<C>> childSupplyMethod;

  /* field name of the parent on the child class */
  private final String parentFieldName;

  /* Method to handle orphaned children of the parent resource */
  private final Consumer<C> orphanHandler;

  /**
   * Handles create operations to link associations of children to a given parent.
   *
   * @param parent parent with children to resolve
   */
  public void onCreate(P parent) {
    OneToManyHibernateHelper.linkChildren(childSupplyMethod.apply(parent), parent, parentApplyMethod);
  }

  /**
   * Handles update operations to resolve associations of children to a given parent.
   *
   * @param parent      parent with children to resolve
   * @param dinaService database service used to resolve current/incoming children
   */
  public void onUpdate(P parent, DinaService<?> dinaService) {
    OneToManyHibernateHelper.handleOrphans(
        OneToManyHibernateHelper.findByParent(childClassType, parentFieldName, parent, dinaService),
        childSupplyMethod.apply(parent),
        orphanHandler
    );
    OneToManyHibernateHelper.linkChildren(childSupplyMethod.apply(parent), parent, parentApplyMethod);
  }

  /**
   * Handles delete operations to resolve orphans a given parent.
   *
   * @param parent parent with orphans to resolve
   */
  public void onDelete(P parent) {
    OneToManyHibernateHelper.handleOrphans(childSupplyMethod.apply(parent), null, orphanHandler);
  }

}
