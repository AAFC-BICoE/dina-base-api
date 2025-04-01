package ca.gc.aafc.dina.dto;

import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Singular;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Represents the meta section of a single DTO
 * Meta section that is inside a Resource Objects
 *
 */
@Builder
@AllArgsConstructor
public class JsonApiDtoMeta {

  public static final String PERMISSIONS_PROVIDER = "permissionsProvider";
  public static final String PERMISSIONS = "permissions";
  public static final String WARNINGS = "warnings";

  private String permissionsProvider;
  private Set<String> permissions;

  @Singular
  private Map<String, Object> warnings;

  /**
   * Call the provided method to set the metadata per key/value.
   * @param metaSetter
   */
  public void populateMeta(BiFunction<String, Object, ?> metaSetter) {
    if (StringUtils.isNotBlank(permissionsProvider)) {
      metaSetter.apply(PERMISSIONS_PROVIDER, permissionsProvider);
    }

    if (CollectionUtils.isNotEmpty(permissions)) {
      metaSetter.apply(PERMISSIONS, permissions);
    }

    if (MapUtils.isNotEmpty(warnings)) {
      metaSetter.apply(WARNINGS, warnings);
    }
  }
}
