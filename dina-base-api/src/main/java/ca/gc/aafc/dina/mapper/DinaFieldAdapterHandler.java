package ca.gc.aafc.dina.mapper;

import io.crnk.core.queryspec.FilterSpec;
import lombok.SneakyThrows;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Handles the mapping of fields between entities and Dto's using {@link DinaFieldAdapter}.
 *
 * @param <D> - Dto class
 */
public class DinaFieldAdapterHandler<D> {

  private final Set<DinaFieldAdapter<Object, Object, Object, Object>> adapters;
  private final Class<? extends D> dtoClass;

  public DinaFieldAdapterHandler(Class<? extends D> dtoClass) {
    this.dtoClass = dtoClass;
    this.adapters = initAdapters(dtoClass);
  }

  @SneakyThrows
  @SuppressWarnings("unchecked")
  private Set<DinaFieldAdapter<Object, Object, Object, Object>> initAdapters(Class<? extends D> dtoClass) {
    Set<DinaFieldAdapter<Object, Object, Object, Object>> set = new HashSet<>();
    if (dtoClass.isAnnotationPresent(CustomFieldAdapter.class)) {
      for (Class<? extends DinaFieldAdapter<?, ?, ?, ?>> aClass :
        dtoClass.getAnnotation(CustomFieldAdapter.class).adapters()) {
        set.add(
          (DinaFieldAdapter<Object, Object, Object, Object>) aClass.getConstructor().newInstance());
      }
    }
    return set;
  }

  /**
   * Maps the selected fields from a source to a target that are tracked by the handler.
   *
   * @param source - source of the mapping.
   * @param target - target of the mapping
   */
  @SneakyThrows
  public void resolveFields(Object source, Object target) {
    for (DinaFieldAdapter<Object, Object, Object, Object> adapt : adapters) {
      if (target.getClass() == dtoClass) {
        adapt.dtoApplyMethod(target).accept(adapt.toDTO(adapt.entitySupplyMethod(source).get()));
      } else {
        adapt.entityApplyMethod(target).accept(adapt.toEntity(adapt.dtoSupplyMethod(source).get()));
      }
    }
  }

  /**
   * Returns the Filter spec apply function for a given field tracked by the handler adapters, or
   * Optional Empty if the field is not tracked.
   *
   * @param field - field with custom filter specs
   * @return the Filter spec apply function for a given field, or Optional empty
   */
  public Optional<Function<Object, FilterSpec[]>> findFilterSpec(String field) {
    return this.adapters.stream()
      .map(DinaFieldAdapter::toFilterSpec)
      .filter(specs -> specs.containsKey(field))
      .findAny()
      .map(spec -> spec.get(field));

  }

}
