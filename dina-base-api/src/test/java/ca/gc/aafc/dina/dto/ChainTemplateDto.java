package ca.gc.aafc.dina.dto;

import ca.gc.aafc.dina.entity.ChainTemplate;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.toedter.spring.hateoas.jsonapi.JsonApiId;
import com.toedter.spring.hateoas.jsonapi.JsonApiTypeForClass;

@Data
@JsonApiTypeForClass(ChainTemplateDto.TYPENAME)
@RelatedEntity(ChainTemplate.class)
public class ChainTemplateDto {

  public static final String TYPENAME = "chainTemplate";

  @JsonApiId
  private UUID uuid;

  private String createdBy;
  private OffsetDateTime createdOn;

  private String group;
  
  private String name;
  
}
