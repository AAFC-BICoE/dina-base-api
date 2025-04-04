package ca.gc.aafc.dina.dto;

import ca.gc.aafc.dina.entity.ComplexObject;
import ca.gc.aafc.dina.entity.Project;
import ca.gc.aafc.dina.entity.Task;
import ca.gc.aafc.dina.repository.meta.JsonApiExternalRelation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.crnk.core.resource.annotations.JsonApiId;
import io.crnk.core.resource.annotations.JsonApiRelation;
import io.crnk.core.resource.annotations.JsonApiResource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.javers.core.metamodel.annotation.PropertyName;
import org.javers.core.metamodel.annotation.TypeName;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@JsonApiResource(type = ProjectDTO.RESOURCE_TYPE)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RelatedEntity(Project.class)
@TypeName(ProjectDTO.RESOURCE_TYPE)
public final class ProjectDTO implements ca.gc.aafc.dina.dto.JsonApiResource {

  public static final String RESOURCE_TYPE = "Project";

  @com.toedter.spring.hateoas.jsonapi.JsonApiId
  @JsonApiId
  @org.javers.core.metamodel.annotation.Id
  @PropertyName("id")
  private UUID uuid;
  private String name;
  private OffsetDateTime createdOn;
  private String createdBy;
  private String alias;

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private List<ComplexObject> nameTranslations;

  @JsonApiRelation
  private TaskDTO task;

  private List<TaskDTO> taskHistory;

  @JsonApiExternalRelation(type = "agent")
  @JsonApiRelation
  private ExternalRelationDto acMetaDataCreator;

  @JsonApiExternalRelation(type = "author")
  @JsonApiRelation
  private ExternalRelationDto originalAuthor;

  @JsonApiExternalRelation(type = "author")
  @JsonApiRelation
  private List<ExternalRelationDto> authors;

  // list of people but not exposed as relationship
  private List<PersonDTO> randomPeople;

  @JsonIgnore
  public UUID getJsonApiId() {
    return uuid;
  }

  @JsonIgnore
  public String getJsonApiType() {
    return RESOURCE_TYPE;
  }
}
