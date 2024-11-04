package ca.gc.aafc.dina.dto;


import java.util.function.BiFunction;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * Represents the meta section of a JSON:API response.
 *
 */
@Builder
@AllArgsConstructor
public final class JsonApiMeta {

  public static String RESOURCE_COUNT = "totalResourceCount";
  public static String MODULE_VERSION = "moduleVersion";

  private Integer totalResourceCount = null;
  private String moduleVersion = null;

  private JsonApiMeta() {
  }

  public void populateMeta(BiFunction<String, Object, ?> metaSetter) {
    if (totalResourceCount != null) {
      metaSetter.apply(RESOURCE_COUNT, totalResourceCount);
    }

    if (moduleVersion != null) {
      metaSetter.apply(MODULE_VERSION, moduleVersion);
    }
  }

}
