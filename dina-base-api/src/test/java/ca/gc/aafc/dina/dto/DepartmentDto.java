package ca.gc.aafc.dina.dto;

import ca.gc.aafc.dina.entity.Department;
import ca.gc.aafc.dina.mapper.CustomFieldAdapter;
import ca.gc.aafc.dina.mapper.DinaFieldAdapter;
import ca.gc.aafc.dina.mapper.IgnoreDinaMapping;
import io.crnk.core.queryspec.FilterSpec;
import io.crnk.core.queryspec.PathSpec;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Data
@JsonApiResource(type = "department")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RelatedEntity(Department.class)
@TypeName("department")
@CustomFieldAdapter(adapters = DepartmentDto.DerivedAdapter.class)
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

  @IgnoreDinaMapping()
  private PersonDTO departmentHead;

  @IgnoreDinaMapping(reason = "simply derived from location")
  private String derivedFromLocation;

  private Department.DepartmentDetails departmentDetails;

  public static class DerivedAdapter implements DinaFieldAdapter<DepartmentDto, Department, String, String> {

    @Override
    public String toDTO(String s) {
      return s;
    }

    @Override
    public String toEntity(String s) {
      return s;
    }

    @Override
    public Consumer<String> entityApplyMethod(Department entityRef) {
      return s -> {
      };// no mapping
    }

    @Override
    public Consumer<String> dtoApplyMethod(DepartmentDto dtoRef) {
      return dtoRef::setDerivedFromLocation;
    }

    @Override
    public Supplier<String> entitySupplyMethod(Department entityRef) {
      return entityRef::getLocation;
    }

    @Override
    public Supplier<String> dtoSupplyMethod(DepartmentDto dtoRef) {
      return () -> null; // no supply
    }

    @Override
    public Map<String, Function<FilterSpec, FilterSpec[]>> toFilterSpec() {
      return Map.of("derivedFromLocation", filterSpec -> new FilterSpec[]{
        PathSpec.of("location").filter(filterSpec.getOperator(), filterSpec.getValue())
      });
    }
  }
}
