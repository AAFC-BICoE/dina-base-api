package ca.gc.aafc.dina.dto;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

/**
 * Contains a DTO with its optional relationships.
 */
@AllArgsConstructor
@Builder
@Getter
public class JsonApiDto<D> {

  private final D dto;

  @Singular
  private final Map<String, RelationshipBase> relationships;

  public interface RelationshipBase {
  }

  @AllArgsConstructor
  @Builder
  @Getter
  public static class RelationshipToOne implements RelationshipBase {
    private JsonApiResource included;
  }

  @AllArgsConstructor
  @Builder
  @Getter
  public static class RelationshipToMany implements RelationshipBase {
    private List<JsonApiResource> included;
  }

  @AllArgsConstructor
  @Builder
  @Getter
  public static class RelationshipToOneExternal implements RelationshipBase {
    private JsonApiExternalResource included;
  }

  @AllArgsConstructor
  @Builder
  @Getter
  public static class RelationshipManyExternal implements RelationshipBase {
    private List<JsonApiExternalResource> included;
  }

}
