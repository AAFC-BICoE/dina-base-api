package ca.gc.aafc.dina.dto;

import java.util.List;

import io.crnk.core.resource.annotations.JsonApiId;
import io.crnk.core.resource.annotations.JsonApiRelation;
import io.crnk.core.resource.annotations.JsonApiResource;
import lombok.Data;

@Data
@JsonApiResource(type = "department")
public class DepartmentDto {

  @JsonApiId
  private Integer id;

  private String name;

  private String location;

  @JsonApiRelation(opposite = "department")
  private List<EmployeeDto> employees;

}