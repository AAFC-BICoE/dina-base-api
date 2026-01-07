package ca.gc.aafc.dina.dto;

import org.javers.core.metamodel.annotation.PropertyName;

import com.toedter.spring.hateoas.jsonapi.JsonApiId;
import com.toedter.spring.hateoas.jsonapi.JsonApiTypeForClass;

import ca.gc.aafc.dina.entity.Item;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RelatedEntity(Item.class)
@JsonApiTypeForClass(ItemDto.TYPE_NAME)
public class ItemDto implements JsonApiResource {

  public static final String TYPE_NAME = "item";

  @JsonApiId
  @org.javers.core.metamodel.annotation.Id
  @PropertyName("id")
  private UUID uuid;

  private String group;
  private String createdBy;
  private OffsetDateTime createdOn;

  @Override
  public String getJsonApiType() {
    return TYPE_NAME;
  }

  @Override
  public UUID getJsonApiId() {
    return uuid;
  }
}
