package ca.gc.aafc.dina.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.toedter.spring.hateoas.jsonapi.JsonApiId;
import com.toedter.spring.hateoas.jsonapi.JsonApiTypeForClass;

import ca.gc.aafc.dina.entity.CarDriver;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RelatedEntity(CarDriver.class)
@JsonApiTypeForClass(CarDriverDto.TYPENAME)
public class CarDriverDto implements ca.gc.aafc.dina.dto.JsonApiResource {

  public static final String TYPENAME = "car-driver";

  @JsonApiId
  private UUID uuid;

  private JsonbCarDto car;

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
