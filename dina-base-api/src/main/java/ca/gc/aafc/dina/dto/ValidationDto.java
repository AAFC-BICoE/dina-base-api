package ca.gc.aafc.dina.dto;

import java.util.Map;

import io.crnk.core.resource.annotations.JsonApiId;
import io.crnk.core.resource.annotations.JsonApiResource;
import lombok.Data;

@JsonApiResource(type = "validation")
@Data
public class ValidationDto {

  @JsonApiId
  private String id;

  private String type;

  private Map<String, Object> data;
  
}
