package ca.gc.aafc.dina.dto;

import ca.gc.aafc.dina.entity.ComplexObject;
import ca.gc.aafc.dina.entity.Person;
import ca.gc.aafc.dina.mapper.CustomFieldAdapter;
import ca.gc.aafc.dina.mapper.DinaFieldAdapter;
import io.crnk.core.queryspec.FilterSpec;
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
  private String customField;

  public static class CustomFieldAdapterImp implements DinaFieldAdapter<PersonDTO, Person, String, ComplexObject> {

    public CustomFieldAdapterImp() {
    }

    @Override
    public String toDTO(ComplexObject complexObject) {
      return complexObject == null ? "" : complexObject.getName();
    }

    @Override
    public ComplexObject toEntity(String s) {
      return s == null ? null : ComplexObject.builder().name(s).build();
    }

    @Override
    public Consumer<ComplexObject> entityApplyMethod(Person entityRef) {
      return entityRef::setCustomField;
    }

    @Override
    public Consumer<String> dtoApplyMethod(PersonDTO dtoRef) {
      return dtoRef::setCustomField;
    }

    @Override
    public FilterSpec[] toFilterSpec() {
      return new FilterSpec[0];
    }
  }
}
