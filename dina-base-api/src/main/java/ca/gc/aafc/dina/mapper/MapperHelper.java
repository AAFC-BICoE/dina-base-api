package ca.gc.aafc.dina.mapper;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import ca.gc.aafc.dina.dto.RelatedEntity;
import ca.gc.aafc.dina.util.ClassAnnotationHelper;
import lombok.NonNull;

/**
 * Utility class to help map DTOs to their related Entities.
 */
public final class MapperHelper {

  private MapperHelper() {
  }

  /**
   * Returns a Map of DTOs to their related Entities given a base class package to
   * scan.
   *
   * @param basePackageClass - Class of the package to scan
   * @throws NullPointerException When the given base package class is null
   * @return Map of DTOs to their related Entities
   */
  public static Map<Class<?>, Class<?>> getDtoToEntityMapping(@NonNull Class<?> basePackageClass) {
    return ClassAnnotationHelper
      .findAnnotatedClasses(basePackageClass, RelatedEntity.class)
      .stream()
      .collect(
        Collectors.toMap(
          Function.identity(),
          clazz -> clazz.getAnnotation(RelatedEntity.class).value()));
  }
}
