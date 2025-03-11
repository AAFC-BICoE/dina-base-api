package ca.gc.aafc.dina.dto;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

/**
 * Contains a DTO with its optional relationships.
 * Used to properly capture a response on the web layer (repository) for getOne and getAll.
 * As opposed to JsonApiModelBuilder, {@link JsonApiDto} is agnostic of the library used to serialize
 * the response.
 */
@AllArgsConstructor
@Builder
@Getter
public class JsonApiDto<D> {

  private final D dto;

  @Singular
  private final Map<String, RelationshipBase> relationships;

  /**
   * Sealed interface since there is only 4 expected type of relationships.
   */
  public sealed interface RelationshipBase permits
    RelationshipToOne, RelationshipToMany, RelationshipToOneExternal, RelationshipManyExternal {
  }

  @AllArgsConstructor
  @Builder
  @Getter
  public static final class RelationshipToOne implements RelationshipBase {
    private JsonApiResource included;
  }

  @AllArgsConstructor
  @Builder
  @Getter
  public static final class RelationshipToMany implements RelationshipBase {
    private List<JsonApiResource> included;
  }

  @AllArgsConstructor
  @Builder
  @Getter
  public static final class RelationshipToOneExternal implements RelationshipBase {
    private JsonApiExternalResource included;
  }

  @AllArgsConstructor
  @Builder
  @Getter
  public static final class RelationshipManyExternal implements RelationshipBase {
    private List<JsonApiExternalResource> included;
  }

}
