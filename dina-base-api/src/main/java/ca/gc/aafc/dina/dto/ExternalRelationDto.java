package ca.gc.aafc.dina.dto;

import org.javers.core.metamodel.annotation.TypeName;
import org.javers.core.metamodel.annotation.Value;

import io.crnk.core.resource.ResourceTypeHolder;
import io.crnk.core.resource.annotations.JsonApiId;
import io.crnk.core.resource.annotations.JsonApiResource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonApiResource(type = "external-type")
@Value // This class is considered a "value" belonging to a parent DTO.
@TypeName(ExternalRelationDto.TYPENAME)
public class ExternalRelationDto implements ResourceTypeHolder {

  public static final String TYPENAME = "external-relation";
  public static final String ID_FIELD_NAME = "id";

  @JsonApiId
  private String id;

  private String type;
}
