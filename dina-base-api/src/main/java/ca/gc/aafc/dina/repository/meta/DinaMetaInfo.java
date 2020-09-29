package ca.gc.aafc.dina.repository.meta;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.crnk.core.resource.meta.DefaultPagedMetaInformation;
import lombok.NonNull;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Meta information to be returned in a JSON response.
 */
public class DinaMetaInfo extends DefaultPagedMetaInformation {

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private Map<String, String> externalTypes;

  public Map<String, String> getExternalTypes() {
    return externalTypes;
  }

  public void setExternalTypes(Map<String, String> externalTypes) {
    this.externalTypes = externalTypes;
  }

  /**
   * Returns a map of a given classes {@link JsonApiExternalRelation} types mapped to their current
   * reference as provided by the given {@link ExternalResourceProvider}
   *
   * @param clazz    - class containing the {@link JsonApiExternalRelation}
   * @param provider - {@link ExternalResourceProvider} providing the mapping
   * @return Returns a map of a given classes {@link JsonApiExternalRelation} types mapped to their
   * current reference
   */
  public static Map<String, String> parseExternalTypes(
    @NonNull Class<?> clazz,
    @NonNull ExternalResourceProvider provider
  ) {
    return FieldUtils.getFieldsListWithAnnotation(clazz, JsonApiExternalRelation.class)
      .stream()
      .map(field -> field.getAnnotation(JsonApiExternalRelation.class).type())
      .distinct()
      .collect(Collectors.toMap(Function.identity(), provider::getReferenceForType));
  }
}
