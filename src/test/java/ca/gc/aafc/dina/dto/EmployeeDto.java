package ca.gc.aafc.dina.dto;

import io.crnk.core.resource.annotations.JsonApiId;
import io.crnk.core.resource.annotations.JsonApiRelation;
import io.crnk.core.resource.annotations.JsonApiResource;
import lombok.Data;

@Data
@JsonApiResource(type = "employee")
public class EmployeeDto {

  @JsonApiId
  private Integer id;

  private String name;

  @JsonApiRelation
  private DepartmentDto department;

}