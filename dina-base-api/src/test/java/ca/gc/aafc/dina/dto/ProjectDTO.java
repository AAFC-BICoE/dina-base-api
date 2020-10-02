package ca.gc.aafc.dina.dto;

import ca.gc.aafc.dina.entity.Project;
import ca.gc.aafc.dina.jsonapi.DinaRepoRestIT;
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
  private DinaRepoRestIT.TaskDTO task;

  @JsonApiExternalRelation(type = "Person")
  @JsonApiRelation
  private ExternalRelationDto acMetaDataCreator;

  @JsonApiExternalRelation(type = "Author")
  @JsonApiRelation
  private ExternalRelationDto originalAuthor;

  @CustomFieldResolver(fieldName = "acMetaDataCreator")
  public static ExternalRelationDto acMetaDataCreatorToDTO(@NonNull Project entity) {
    return entity.getAcMetaDataCreator() == null ? null : ExternalRelationDto.builder()
      .id(entity.getAcMetaDataCreator().toString())
      .build();
  }

  @CustomFieldResolver(fieldName = "acMetaDataCreator")
  public static UUID acMetaDataCreatorToEntity(@NonNull ProjectDTO dto) {
    return dto.getAcMetaDataCreator() ==
           null ? null : UUID.fromString((String) dto.getAcMetaDataCreator().getId());
  }

  @CustomFieldResolver(fieldName = "originalAuthor")
  public static ExternalRelationDto originalAuthorToDTO(@NonNull Project entity) {
    return entity.getOriginalAuthor() == null ? null : ExternalRelationDto.builder()
      .id(entity.getOriginalAuthor().toString())
      .build();
  }

  @CustomFieldResolver(fieldName = "originalAuthor")
  public static UUID originalAuthorToEntity(@NonNull ProjectDTO dto) {
    return dto.getOriginalAuthor() == null ? null : UUID.fromString((String) dto.getOriginalAuthor()
      .getId());
  }
}
