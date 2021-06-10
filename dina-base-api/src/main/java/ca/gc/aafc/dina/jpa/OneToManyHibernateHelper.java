package ca.gc.aafc.dina.jpa;

import ca.gc.aafc.dina.entity.DinaEntity;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class OneToManyHibernateHelper {

  private OneToManyHibernateHelper() {
  }

  public static <E extends DinaEntity> void resolveChildren(
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
}
