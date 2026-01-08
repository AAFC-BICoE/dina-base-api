package ca.gc.aafc.dina.dto;

import org.javers.core.metamodel.annotation.DiffIgnore;
import org.javers.core.metamodel.annotation.Id;
import org.javers.core.metamodel.annotation.PropertyName;
import org.javers.core.metamodel.annotation.TypeName;

import ca.gc.aafc.dina.entity.Employee;
import ca.gc.aafc.dina.mapper.IgnoreDinaMapping;
import io.crnk.core.resource.annotations.JsonApiRelation;
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
public class EmployeeDto {

  public static final String TYPENAME = "employee";

  @Id
  @PropertyName("id")
  private Integer id;

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
}