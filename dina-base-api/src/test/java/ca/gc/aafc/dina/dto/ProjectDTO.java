package ca.gc.aafc.dina.dto;

import ca.gc.aafc.dina.entity.Project;
import ca.gc.aafc.dina.mapper.CustomFieldResolver;
import ca.gc.aafc.dina.repository.meta.JsonApiExternalRelation;
import io.crnk.core.resource.annotations.JsonApiId;
import io.crnk.core.resource.annotations.JsonApiRelation;
import io.crnk.core.resource.annotations.JsonApiResource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.javers.core.metamodel.annotation.PropertyName;
import org.javers.core.metamodel.annotation.TypeName;

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

  @JsonApiRelation
  private TaskDTO task;

  @JsonApiExternalRelation(type = "Agent")
  @JsonApiRelation
  private AgentExternalDTO acMetaDataCreator;

  @JsonApiExternalRelation(type = "Author")
  @JsonApiRelation
  private AuthorExternalDTO originalAuthor;

  @CustomFieldResolver(fieldName = "acMetaDataCreator")
  public static AgentExternalDTO acMetaDataCreatorToDTO(@NonNull Project entity) {
    return entity.getAcMetaDataCreator() == null ? null : AgentExternalDTO.builder()
      .id(entity.getAcMetaDataCreator().toString())
      .build();
  }

  @CustomFieldResolver(fieldName = "acMetaDataCreator")
  public static UUID acMetaDataCreatorToEntity(@NonNull ProjectDTO dto) {
    return dto.getAcMetaDataCreator() == null ? null : UUID.fromString(dto.getAcMetaDataCreator()
      .getId());
  }

  @CustomFieldResolver(fieldName = "originalAuthor")
  public static AuthorExternalDTO originalAuthorToDTO(@NonNull Project entity) {
    return entity.getOriginalAuthor() == null ? null : AuthorExternalDTO.builder()
      .id(entity.getOriginalAuthor().toString())
      .build();
  }

  @CustomFieldResolver(fieldName = "originalAuthor")
  public static UUID originalAuthorToEntity(@NonNull ProjectDTO dto) {
    return dto.getOriginalAuthor() == null ? null : UUID.fromString(dto.getOriginalAuthor()
      .getId());
  }
}
