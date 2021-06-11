package ca.gc.aafc.dina.jpa;

import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.service.DinaService;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

@RequiredArgsConstructor
public class OneToManyFieldHandler<C extends DinaEntity, P> {

  private final Class<C> childClassType;
  private final Function<C, Consumer<P>> parentApplyMethod;
  private final Function<P, List<C>> childSupplyMethod;
  private final String parentFieldName;
  private final Consumer<C> orphanHandler;

  public void onCreate(P parent) {
    OneToManyHibernateHelper.linkChildren(childSupplyMethod.apply(parent), parent, parentApplyMethod);
  }

  public void onUpdate(P parent, DinaService<?> dinaService) {
    OneToManyHibernateHelper.handleOrphans(
      OneToManyHibernateHelper.findByParent(childClassType, parentFieldName, parent, dinaService),
      childSupplyMethod.apply(parent),
      orphanHandler
    );
    OneToManyHibernateHelper.linkChildren(childSupplyMethod.apply(parent), parent, parentApplyMethod);
  }

  public void onDelete(P parent) {
    OneToManyHibernateHelper.handleOrphans(childSupplyMethod.apply(parent), null, orphanHandler);
  }

}
