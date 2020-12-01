package ca.gc.aafc.dina.dto;

import java.util.List;
import java.util.UUID;

import ca.gc.aafc.dina.entity.Department;
import ca.gc.aafc.dina.mapper.IgnoreDinaMapping;
import io.crnk.core.resource.annotations.JsonApiId;
import io.crnk.core.resource.annotations.JsonApiRelation;
import io.crnk.core.resource.annotations.JsonApiResource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.javers.core.metamodel.annotation.Id;
import org.javers.core.metamodel.annotation.PropertyName;
import org.javers.core.metamodel.annotation.TypeName;

@Data
@JsonApiResource(type = "department")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RelatedEntity(Department.class)
@TypeName("department")
public class DepartmentDto {

  @JsonApiId
  @Id
  @PropertyName("id")
  private UUID uuid;

  private String name;

  private String location;

  @IgnoreDinaMapping
  private Integer employeeCount;

  @JsonApiRelation(mappedBy = "department")
  private List<EmployeeDto> employees;

  private PersonDTO departmentHead;

}