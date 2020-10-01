package ca.gc.aafc.dina.dto;

import io.crnk.core.resource.annotations.JsonApiId;
import io.crnk.core.resource.annotations.JsonApiResource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@JsonApiResource(type = ExternalRelationDto.RESOURCE_TYPE)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalRelationDto {
  public static final String RESOURCE_TYPE = "external-relation";
  @JsonApiId
  private Serializable id;
}
