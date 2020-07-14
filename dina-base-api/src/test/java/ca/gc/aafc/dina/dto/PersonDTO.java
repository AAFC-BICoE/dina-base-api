package ca.gc.aafc.dina.dto;

import java.util.List;
import java.util.UUID;

import ca.gc.aafc.dina.entity.Person;
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
@RelatedEntity(Person.class)
public class PersonDTO {

  @JsonApiId
  private UUID uuid;

  private String name;

  private String group;

  private String[] nickNames;

  @JsonApiRelation
  private DepartmentDto department;

  @JsonApiRelation
  private List<DepartmentDto> departments;

}
