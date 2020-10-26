package ca.gc.aafc.dina.dto;

import ca.gc.aafc.dina.entity.ComplexObject;
import ca.gc.aafc.dina.entity.Project;
import ca.gc.aafc.dina.repository.meta.JsonApiExternalRelation;
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
public final class ProjectDTO {

  public static final String RESOURCE_TYPE = "Project";

  @JsonApiId
  @org.javers.core.metamodel.annotation.Id
  @PropertyName("id")
  private UUID uuid;
  private String name;
  private OffsetDateTime createdOn;
  private String createdBy;

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private List<ComplexObject> nameTranslations;

  @JsonApiRelation
  private TaskDTO task;

  @JsonApiExternalRelation(type = "agent")
  @JsonApiRelation
  private ExternalRelationDto acMetaDataCreator;

  @JsonApiExternalRelation(type = "author")
  @JsonApiRelation
  private ExternalRelationDto originalAuthor;

}
