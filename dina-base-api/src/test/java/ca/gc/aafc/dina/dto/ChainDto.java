package ca.gc.aafc.dina.dto;

import ca.gc.aafc.dina.entity.Chain;
import ca.gc.aafc.dina.repository.meta.JsonApiExternalRelation;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.toedter.spring.hateoas.jsonapi.JsonApiId;
import com.toedter.spring.hateoas.jsonapi.JsonApiTypeForClass;

@Data
@JsonApiTypeForClass(ChainDto.TYPENAME)
@RelatedEntity(Chain.class)
public class ChainDto {

  public static final String TYPENAME = "chain";

  @JsonApiId
  private UUID uuid;

  private String createdBy;
  private OffsetDateTime createdOn;

  private String group;

  private String name;

  private ChainTemplateDto chainTemplate;

  @JsonApiExternalRelation(type = "agent")
  private ExternalRelationDto agent;

}
