package ca.gc.aafc.dina.dto;

import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.toedter.spring.hateoas.jsonapi.JsonApiId;
import com.toedter.spring.hateoas.jsonapi.JsonApiTypeForClass;

import ca.gc.aafc.dina.entity.JsonbMethod;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RelatedEntity(JsonbMethod.class)
@JsonApiTypeForClass(JsonbMethodDto.TYPENAME)
public class JsonbMethodDto implements ca.gc.aafc.dina.dto.JsonApiResource {

  public static final String TYPENAME = "jsonb-method";

  @JsonApiId
  private UUID uuid;
  private Map<String, Object> jsonData;

  @Override
  @JsonIgnore
  public String getJsonApiType() {
    return TYPENAME;
  }

  @Override
  @JsonIgnore
  public UUID getJsonApiId() {
    return uuid;
  }
}
