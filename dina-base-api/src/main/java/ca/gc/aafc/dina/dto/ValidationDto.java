package ca.gc.aafc.dina.dto;

import com.fasterxml.jackson.databind.JsonNode;
import io.crnk.core.resource.annotations.JsonApiId;
import io.crnk.core.resource.annotations.JsonApiResource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonApiResource(type = "validation")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationDto {

  @JsonApiId
  private String id;

  private String type;

  private JsonNode data;

}
