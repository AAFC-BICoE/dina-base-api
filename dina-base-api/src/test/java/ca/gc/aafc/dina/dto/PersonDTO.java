package ca.gc.aafc.dina.dto;

import java.util.UUID;

import io.crnk.core.resource.annotations.JsonApiId;
import io.crnk.core.resource.annotations.JsonApiRelation;
import io.crnk.core.resource.annotations.JsonApiResource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@JsonApiResource(type = "person")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonDTO {

  @JsonApiId
  private UUID uuid;

  private String name;

  @JsonApiRelation
  private DepartmentDto department;

}
