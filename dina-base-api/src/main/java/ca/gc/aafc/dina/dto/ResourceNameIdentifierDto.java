package ca.gc.aafc.dina.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Identifies a resource by name.
 * name may not be public depending on the implementation.
 * group is optional depending on the implementation.
 */
@Builder
@Getter
public class ResourceNameIdentifierDto {

  private String type;
  private String name;
  private String group;

}
