package ca.gc.aafc.dina.dto;

import java.util.UUID;

import org.javers.core.metamodel.annotation.DiffIgnore;
import org.javers.core.metamodel.annotation.Id;
import org.javers.core.metamodel.annotation.PropertyName;
import org.javers.core.metamodel.annotation.TypeName;

import com.fasterxml.jackson.annotation.JsonIgnore;

import ca.gc.aafc.dina.entity.Employee;
import ca.gc.aafc.dina.mapper.IgnoreDinaMapping;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RelatedEntity(Employee.class)
@TypeName(EmployeeDto.TYPENAME)
public class EmployeeDto implements ca.gc.aafc.dina.dto.JsonApiResource {

  public static final String TYPENAME = "employee";

  @com.toedter.spring.hateoas.jsonapi.JsonApiId
  @Id
  @PropertyName("id")
  private UUID uuid;

  private String name;

  /** This field is generated from the name field. */
  @IgnoreDinaMapping(reason = "field is generated from name")
  private String nameUppercase;

  private String job;

  @IgnoreDinaMapping()
  private String customField;
  
  @JsonApiRelation
  @DiffIgnore
  private DepartmentDto department;

  @JsonApiRelation
  private PersonDTO manager;

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