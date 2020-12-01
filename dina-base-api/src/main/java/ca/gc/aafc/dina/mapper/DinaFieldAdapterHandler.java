package ca.gc.aafc.dina.mapper;

import lombok.SneakyThrows;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Handles the mapping of fields between entities and Dto's using {@link DinaFieldAdapter}.
 *
 * @param <D> - Dto class
 */
public class DinaFieldAdapterHandler<D> {

  private final Map<String, DinaFieldAdapter<Object, Object, Object, Object>> adaptersPerField;
  private final Class<? extends D> dtoClass;

  public DinaFieldAdapterHandler(Class<? extends D> dtoClass) {
    this.dtoClass = dtoClass;
    adaptersPerField = Arrays
      .stream(FieldUtils.getFieldsWithAnnotation(dtoClass, CustomFieldAdapter.class))
      .collect(Collectors.toMap(Field::getName, this::makeFieldAdapter));
  }

  @SneakyThrows
  @SuppressWarnings("unchecked")
  private DinaFieldAdapter<Object, Object, Object, Object> makeFieldAdapter(Field field) {
    return (DinaFieldAdapter<Object, Object, Object, Object>) field
      .getAnnotation(CustomFieldAdapter.class).adapter().getConstructor().newInstance();
  }

  /**
   * Maps all fields from a source to a target that are tracked by the handler.
   *
   * @param source - source of the mapping.
   * @param target - target of the mapping
   */
  public void resolveFields(Object source, Object target) {
    this.resolveFields(this.adaptersPerField.keySet(), source, target);
  }

  /**
   * Maps the selected fields from a source to a target that are tracked by the handler.
   *
   * @param selectedFields - fields to map.
   * @param source         - source of the mapping.
   * @param target         - target of the mapping
   */
  @SneakyThrows
  public void resolveFields(Set<String> selectedFields, Object source, Object target) {
    for (Map.Entry<String, DinaFieldAdapter<Object, Object, Object, Object>> entry :
      adaptersPerField.entrySet()) {

      String field = entry.getKey();
      DinaFieldAdapter<Object, Object, Object, Object> dinaFieldAdapter = entry.getValue();

      if (selectedFields.contains(field)) {
        if (target.getClass() == dtoClass) {
          dinaFieldAdapter.dtoApplyMethod(target)
            .accept(dinaFieldAdapter.toDTO(dinaFieldAdapter.entitySupplyMethod(source).get()));
        } else {
          dinaFieldAdapter.entityApplyMethod(target)
            .accept(dinaFieldAdapter.toEntity(dinaFieldAdapter.dtoSupplyMethod(source).get()));
        }
      }
    }
  }

  /**
   * Returns true if this handler is tracking a given field name.
   *
   * @param fieldName - field name to check.
   * @return true if this field adapter is tracking a given field name.
   */
  public boolean hasFieldAdapter(String fieldName) {
    return this.adaptersPerField.keySet().stream().anyMatch(fieldName::equalsIgnoreCase);
  }

}
