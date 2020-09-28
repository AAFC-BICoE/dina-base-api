package ca.gc.aafc.dina.repository.meta;

import io.crnk.core.resource.meta.DefaultPagedMetaInformation;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DinaMetaInfo extends DefaultPagedMetaInformation {

  private Map<String, String> externalTypes;

  public Map<String, String> getExternalTypes() {
    return externalTypes;
  }

  public void setExternalTypes(Map<String, String> externalTypes) {
    this.externalTypes = externalTypes;
  }

  public static Map<String, String> parseExternalTypes(
    Class<?> clazz,
    ExternalResourceProvider provider
  ) {
    return FieldUtils.getFieldsListWithAnnotation(clazz, JsonApiExternalRelation.class)
      .stream()
      .map(field -> field.getAnnotation(JsonApiExternalRelation.class).type())
      .distinct()
      .collect(Collectors.toMap(Function.identity(), provider::getRelationsForType));
  }
}
