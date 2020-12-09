package ca.gc.aafc.dina.repository.meta;

import ca.gc.aafc.dina.repository.external.ExternalResourceProvider;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.crnk.core.resource.meta.DefaultPagedMetaInformation;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Meta information to be returned in a JSON response.
 */
@Getter
@Setter
public class DinaMetaInfo extends DefaultPagedMetaInformation {

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private List<Map<String, String>> external;

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private String moduleVersion;

  /**
   * Returns a map of a given classes {@link JsonApiExternalRelation} types mapped to their current
   * reference as provided by the given {@link ExternalResourceProvider}
   *
   * @param clazz    - class containing the {@link JsonApiExternalRelation}
   * @param provider - {@link ExternalResourceProvider} providing the mapping
   * @return Returns a map of a given classes {@link JsonApiExternalRelation} types mapped to their
   * current reference
   */
  public static List<Map<String, String>> parseExternalTypes(
    @NonNull Class<?> clazz,
    @NonNull ExternalResourceProvider provider
  ) {
    return FieldUtils.getFieldsListWithAnnotation(clazz, JsonApiExternalRelation.class)
      .stream()
      .map(field -> field.getAnnotation(JsonApiExternalRelation.class).type())
      .distinct()
      .map(s -> Map.of("type", s, "href", provider.getReferenceForType(s)))
      .collect(Collectors.toList());
  }

}
