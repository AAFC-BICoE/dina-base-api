package ca.gc.aafc.dina.dto;

import org.javers.core.metamodel.annotation.Id;
import org.javers.core.metamodel.annotation.PropertyName;
import org.javers.core.metamodel.annotation.TypeName;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.toedter.spring.hateoas.jsonapi.JsonApiTypeForClass;

import ca.gc.aafc.dina.entity.Person;
import ca.gc.aafc.dina.jsonapi.JsonApiImmutable;
import ca.gc.aafc.dina.mapper.IgnoreDinaMapping;

import java.time.OffsetDateTime;
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
@RelatedEntity(Person.class)
@TypeName(PersonDTO.TYPE_NAME)
@JsonApiTypeForClass(PersonDTO.TYPE_NAME)
public class PersonDTO implements ca.gc.aafc.dina.dto.JsonApiResource {

  public static final String TYPE_NAME = "person";
  public static final String CONSTANT = "HAS CONSTANT";

  @com.toedter.spring.hateoas.jsonapi.JsonApiId
  @Id
  @PropertyName("id")
  private UUID uuid;

  private String name;

  private Integer room;

  @JsonApiImmutable(JsonApiImmutable.ImmutableOn.UPDATE)
  private String group;

  private String[] nickNames;

  private OffsetDateTime createdOn;

  //@JsonApiRelation
  private DepartmentDto department;

 // @JsonApiRelation
  private List<DepartmentDto> departmentsHeadBackup;

  @IgnoreDinaMapping(reason = "derived from name + / + group")
  private String customField;

  @JsonApiCalculatedAttribute
  private String expensiveToCompute;

  private String augmentedData;

  @JsonIgnore
  public UUID getJsonApiId() {
    return uuid;
  }

  @JsonIgnore
  public String getJsonApiType() {
    return TYPE_NAME;
  }

}
