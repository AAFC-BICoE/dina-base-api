package ca.gc.aafc.dina.dto;

import ca.gc.aafc.dina.entity.Task;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.javers.core.metamodel.annotation.PropertyName;
import org.javers.core.metamodel.annotation.TypeName;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RelatedEntity(Task.class)
@TypeName(TaskDTO.RESOURCE_TYPE)
public final class TaskDTO implements JsonApiResource {

  public static final String RESOURCE_TYPE = "Task";

  @com.toedter.spring.hateoas.jsonapi.JsonApiId
  @org.javers.core.metamodel.annotation.Id
  @PropertyName("id")
  private UUID uuid;

  private int powerLevel;

  private Integer power;

  @JsonIgnore
  public UUID getJsonApiId() {
    return uuid;
  }

  @JsonIgnore
  public String getJsonApiType() {
    return RESOURCE_TYPE;
  }

}
