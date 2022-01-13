package ca.gc.aafc.dina.dto;

import ca.gc.aafc.dina.entity.Person;
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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Data
@JsonApiResource(type = PersonDTO.TYPE_NAME)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RelatedEntity(Person.class)
@TypeName(PersonDTO.TYPE_NAME)
@CustomFieldAdapter(adapters = PersonDTO.CustomFieldAdapterImp.class)
public class PersonDTO {

  public static final String TYPE_NAME = "person";
  public static final String CONSTANT = "HAS CONSTANT";

  @JsonApiId
  @Id
  @PropertyName("id")
  private UUID uuid;

  private String name;

  private Integer room;

  private String group;

  private String[] nickNames;

  private OffsetDateTime createdOn;

  @JsonApiRelation
  private DepartmentDto department;

  @JsonApiRelation
  private List<DepartmentDto> departments;

  @IgnoreDinaMapping(reason = "derived from name + / + group")
  private String customField;

  public static class CustomFieldAdapterImp implements DinaFieldAdapter<PersonDTO, Person, String, String> {

    public CustomFieldAdapterImp() {
    }

    @Override
    public String toDTO(String entValue) {
      return entValue;
    }

    @Override
    public String toEntity(String dtoValue) {
      return null; // not mapping to anything
    }

    @Override
    public Consumer<String> entityApplyMethod(Person entityRef) {
      return s -> {
      }; // not mapping to anything
    }

    @Override
    public Consumer<String> dtoApplyMethod(PersonDTO dtoRef) {
      return dtoRef::setCustomField;
    }

    @Override
    public Supplier<String> entitySupplyMethod(Person entityRef) {
      return () -> entityRef.getName() + "/" + entityRef.getGroup();
    }

    @Override
    public Supplier<String> dtoSupplyMethod(PersonDTO dtoRef) {
      return dtoRef::getCustomField;
    }

    @Override
    public Map<String, Function<FilterSpec, FilterSpec[]>> toFilterSpec() {
      return Map.of("customField", spec -> {
        Object value = spec.getValue();
        if (value instanceof String) {
          String[] split = ((String) value).split("/");
          return new FilterSpec[]{
            PathSpec.of("name").filter(spec.getOperator(), split[0]),
            PathSpec.of("group").filter(spec.getOperator(), split[1])};
        } else {
          throw new IllegalArgumentException("value must be a String");
        }
      });
    }
  }
}
