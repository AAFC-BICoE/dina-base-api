package ca.gc.aafc.dina.dto;

import java.util.List;

import ca.gc.aafc.dina.mapper.DerivedDtoField;
import io.crnk.core.resource.annotations.JsonApiId;
import io.crnk.core.resource.annotations.JsonApiRelation;
import io.crnk.core.resource.annotations.JsonApiResource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@JsonApiResource(type = "department")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentDto {

  @JsonApiId
  private Integer id;

  private String name;

  private String location;

  @DerivedDtoField
  private Integer employeeCount;

  @JsonApiRelation(opposite = "department")
  private List<EmployeeDto> employees;

}