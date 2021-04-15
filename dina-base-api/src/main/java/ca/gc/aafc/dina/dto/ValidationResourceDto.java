package ca.gc.aafc.dina.dto;

import io.crnk.core.resource.annotations.JsonApiId;
import io.crnk.core.resource.annotations.JsonApiResource;

@JsonApiResource(type = "validation")
public class ValidationResourceDto {

  @JsonApiId
  private String id;

  private Object object;
  
}
