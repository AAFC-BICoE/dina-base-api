package ca.gc.aafc.dina.dto;

import ca.gc.aafc.dina.entity.Person;
import ca.gc.aafc.dina.mapper.CustomFieldAdapter;
import ca.gc.aafc.dina.mapper.DinaFieldAdapter;
import io.crnk.core.queryspec.FilterOperator;
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
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

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

  private OffsetDateTime createdOn;

  @JsonApiRelation
  private DepartmentDto department;

  @JsonApiRelation
  private List<DepartmentDto> departments;

  @CustomFieldAdapter(adapter = CustomFieldAdapterImp.class)
  private Integer customField;

  public static class CustomFieldAdapterImp implements DinaFieldAdapter<PersonDTO, Person, Integer, String> {

    public CustomFieldAdapterImp() {
    }

    @Override
    public Integer toDTO(String s) {
      return s == null ? null : Integer.valueOf(s);
    }

    @Override
    public String toEntity(Integer integer) {
      return integer == null ? null : Integer.toString(integer);
    }

    @Override
    public Consumer<String> entityApplyMethod(Person entityRef) {
      return entityRef::setCustomField;
    }

    @Override
    public Consumer<Integer> dtoApplyMethod(PersonDTO dtoRef) {
      return dtoRef::setCustomField;
    }

    @Override
    public FilterSpec[] toFilterSpec(Object value) {
      if (value instanceof Integer) {
        return Stream.of(
          PathSpec.of("customField").filter(FilterOperator.EQ, Integer.toString((Integer) value))
        ).toArray(FilterSpec[]::new);
      } else {
        throw new IllegalArgumentException("value must be a Integer");
      }
    }
  }
}
