package ca.gc.aafc.dina.dto;

import ca.gc.aafc.dina.entity.Task;
import io.crnk.core.resource.annotations.JsonApiId;
import io.crnk.core.resource.annotations.JsonApiResource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.javers.core.metamodel.annotation.PropertyName;
import org.javers.core.metamodel.annotation.TypeName;

import java.util.UUID;

@Data
@JsonApiResource(type = TaskDTO.RESOURCE_TYPE)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RelatedEntity(Task.class)
@TypeName(TaskDTO.RESOURCE_TYPE)
public final class TaskDTO {
  public static final String RESOURCE_TYPE = "Task";
  @JsonApiId
  @org.javers.core.metamodel.annotation.Id
  @PropertyName("id")
  private UUID uuid;
  private int powerLevel;

  private Integer power;
}
