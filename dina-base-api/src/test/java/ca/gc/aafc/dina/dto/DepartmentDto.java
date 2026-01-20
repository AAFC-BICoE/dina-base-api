package ca.gc.aafc.dina.dto;

import org.javers.core.metamodel.annotation.Id;
import org.javers.core.metamodel.annotation.PropertyName;
import org.javers.core.metamodel.annotation.TypeName;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.toedter.spring.hateoas.jsonapi.JsonApiTypeForClass;

import ca.gc.aafc.dina.entity.Department;
import ca.gc.aafc.dina.mapper.IgnoreDinaMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RelatedEntity(Department.class)
@TypeName(DepartmentDto.TYPE_NAME)
@JsonApiTypeForClass(DepartmentDto.TYPE_NAME)
public class DepartmentDto implements ca.gc.aafc.dina.dto.JsonApiResource {

  public static final String TYPE_NAME = "department";

  @com.toedter.spring.hateoas.jsonapi.JsonApiId
  @Id
  @PropertyName("id")
  private UUID uuid;

  private String name;

  private String location;

  @IgnoreDinaMapping
  private Integer employeeCount;

  @JsonApiRelation
  private List<EmployeeDto> employees;

  @IgnoreDinaMapping()
  private PersonDTO departmentHead;

  @JsonApiRelation
  private PersonDTO departmentOwner;

  @IgnoreDinaMapping(reason = "simply derived from location")
  private String derivedFromLocation;

  private Department.DepartmentDetails departmentDetails;

  @Builder.Default
  private List<Department.DepartmentAlias> aliases = new ArrayList<>();

  private String establishedOn;

  @Override
  @JsonIgnore
  public String getJsonApiType() {
    return TYPE_NAME;
  }

  @Override
  @JsonIgnore
  public UUID getJsonApiId() {
    return uuid;
  }
}
