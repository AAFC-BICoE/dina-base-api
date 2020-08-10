package ca.gc.aafc.dina.dto;

import java.util.List;
import java.util.UUID;

import org.javers.core.metamodel.annotation.Id;
import org.javers.core.metamodel.annotation.PropertyName;
import org.javers.core.metamodel.annotation.TypeName;

import ca.gc.aafc.dina.entity.Person;
import io.crnk.core.resource.annotations.JsonApiId;
import io.crnk.core.resource.annotations.JsonApiRelation;
import io.crnk.core.resource.annotations.JsonApiResource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@JsonApiResource(type = PersonDTO.TYPE_NAME)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RelatedEntity(Person.class)
@TypeName(PersonDTO.TYPE_NAME)
public class PersonDTO {

  public static final String TYPE_NAME = "person";
  public static final String CONSTANT = "HAS CONSTANT";

  @JsonApiId
  @Id
  @PropertyName("id")
  private UUID uuid;

  private String name;

  private String group;

  private String[] nickNames;

  @JsonApiRelation
  private DepartmentDto department;

  @JsonApiRelation
  private List<DepartmentDto> departments;

}
