package ca.gc.aafc.dina.dto;

import org.javers.core.metamodel.annotation.TypeName;
import org.javers.core.metamodel.annotation.Value;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.toedter.spring.hateoas.jsonapi.JsonApiId;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Value // This class is considered a "value" belonging to a parent DTO.
@TypeName(ExternalRelationDto.TYPENAME)
public class ExternalRelationDto {

  public static final String TYPENAME = "external-relation";
  public static final String ID_FIELD_NAME = "id";

  @JsonApiId
  private String id;

  private String type;
}
