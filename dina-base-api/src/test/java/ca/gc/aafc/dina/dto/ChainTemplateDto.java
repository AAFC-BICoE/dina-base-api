package ca.gc.aafc.dina.dto;

import ca.gc.aafc.dina.entity.ChainTemplate;
import io.crnk.core.resource.annotations.JsonApiId;
import io.crnk.core.resource.annotations.JsonApiResource;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@JsonApiResource(type = "chainTemplate")
@RelatedEntity(ChainTemplate.class)
public class ChainTemplateDto {
  
  @JsonApiId
  private UUID uuid;

  private String createdBy;
  private OffsetDateTime createdOn;

  private String group;
  
  private String name;
  
}
