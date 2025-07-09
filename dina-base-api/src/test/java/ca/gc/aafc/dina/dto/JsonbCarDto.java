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

import ca.gc.aafc.dina.entity.JsonbCar;

/**
 * Mostly used to to test jsonb
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RelatedEntity(JsonbCar.class)
@JsonApiTypeForClass(JsonbCarDto.TYPENAME)
public class JsonbCarDto implements ca.gc.aafc.dina.dto.JsonApiResource {

  public static final String TYPENAME = "jsonbcar";

  @JsonApiId
  private UUID uuid;
  private Map<String, Object> jsonData;
  private Map<String, Object> jsonDataMethodDefined;

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
