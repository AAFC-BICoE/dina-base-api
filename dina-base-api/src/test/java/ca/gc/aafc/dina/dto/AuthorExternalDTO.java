package ca.gc.aafc.dina.dto;

import io.crnk.core.resource.annotations.JsonApiId;
import io.crnk.core.resource.annotations.JsonApiResource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@JsonApiResource(type = AuthorExternalDTO.RESOURCE_TYPE)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorExternalDTO implements ExternalRelationDto {
  public static final String RESOURCE_TYPE = "author";

  @JsonApiId
  private String id;

}
