package ca.gc.aafc.dina.mapper;

import lombok.SneakyThrows;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class DinaFieldAdapterHandler<D> {

  private final Map<String, DinaFieldAdapter<Object, Object, Object, Object>> adaptersPerField;
  private final Class<? extends D> dtoClass;

  public DinaFieldAdapterHandler(Class<? extends D> dtoClass) {
    this.dtoClass = dtoClass;
    adaptersPerField = initResolvers(dtoClass);
  }

  private Map<String, DinaFieldAdapter<Object, Object, Object, Object>> initResolvers(Class<? extends D> dtoClass) {
    return Arrays.stream(FieldUtils.getFieldsWithAnnotation(dtoClass, CustomFieldAdapter.class))
      .collect(Collectors.toMap(Field::getName, this::makeFieldAdapter));
  }

  @SneakyThrows
  @SuppressWarnings("unchecked")
  private DinaFieldAdapter<Object, Object, Object, Object> makeFieldAdapter(Field field) {
    return (DinaFieldAdapter<Object, Object, Object, Object>) field
      .getAnnotation(CustomFieldAdapter.class).adapter().getConstructor().newInstance();
  }

  public void resolveFields(Object source, Object target) {
    this.resolveFields(this.adaptersPerField.keySet(), source, target);
  }

  @SneakyThrows
  public void resolveFields(Set<String> selectedFields, Object source, Object target) {
    for (Map.Entry<String, DinaFieldAdapter<Object, Object, Object, Object>> entry :
      adaptersPerField.entrySet()) {

      String field = entry.getKey();
      DinaFieldAdapter<Object, Object, Object, Object> dinaFieldAdapter = entry.getValue();

      if (selectedFields.contains(field)) {
        if (target.getClass() == dtoClass) {
          dinaFieldAdapter
            .dtoApplyMethod(target)
            .accept(dinaFieldAdapter.toDTO(PropertyUtils.getProperty(source, field)));
        } else {
          dinaFieldAdapter
            .entityApplyMethod(target)
            .accept(dinaFieldAdapter.toEntity(PropertyUtils.getProperty(source, field)));
        }
      }
    }
  }

}
