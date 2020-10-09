package ca.gc.aafc.dina.dto;

import io.crnk.core.resource.ResourceTypeHolder;
import io.crnk.core.resource.annotations.JsonApiId;
import io.crnk.core.resource.annotations.JsonApiResource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonApiResource(type = "external-type")
public class ExternalRelationDto implements ResourceTypeHolder {

  @JsonApiId
  private String id;

  private String type;
}
