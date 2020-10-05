package ca.gc.aafc.dina.dto;

import io.crnk.core.resource.annotations.JsonApiId;
import io.crnk.core.resource.annotations.JsonApiResource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@JsonApiResource(type = AgentExternalDTO.RESOURCE_TYPE)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentExternalDTO implements ExternalRelationDto {
  public static final String RESOURCE_TYPE = "agent";

  @JsonApiId
  private String id;
}
