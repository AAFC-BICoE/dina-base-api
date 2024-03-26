package ca.gc.aafc.dina.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import org.apache.commons.collections.CollectionUtils;

/**
 * Identifies a request for resource identifier by name.
 * name may not be unique depending on the implementation.
 * group is optional depending on the implementation.
 */
@Builder
@Getter
public class ResourceNameIdentifierRequestDto {

  private String type;
  private String group;

  @Singular
  private List<String> names;

  public boolean isSingleName() {
    return CollectionUtils.isNotEmpty(names) && names.size() == 1;
  }

  public String getSingleName() {
    if (isSingleName()) {
      return names.get(0);
    }
    return null;
  }

}
