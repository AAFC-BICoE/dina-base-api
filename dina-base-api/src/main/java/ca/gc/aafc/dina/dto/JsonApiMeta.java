package ca.gc.aafc.dina.dto;


import java.util.function.BiFunction;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * Represents the meta section of a JSON:API response.
 * Top-Level meta section.
 *
 */
@Builder
@AllArgsConstructor
public final class JsonApiMeta {

  public static final String RESOURCE_COUNT = "totalResourceCount";
  public static final String MODULE_VERSION = "moduleVersion";

  private Integer totalResourceCount = null;
  private String moduleVersion = null;

  private JsonApiMeta() {
  }

  /**
   * Call the provided method to set the metadata per key/value.
   * @param metaSetter
   */
  public void populateMeta(BiFunction<String, Object, ?> metaSetter) {
    if (totalResourceCount != null) {
      metaSetter.apply(RESOURCE_COUNT, totalResourceCount);
    }

    if (moduleVersion != null) {
      metaSetter.apply(MODULE_VERSION, moduleVersion);
    }
  }

}
