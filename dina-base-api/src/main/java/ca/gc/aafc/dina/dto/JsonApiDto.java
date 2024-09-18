package ca.gc.aafc.dina.dto;

import java.util.List;
import java.util.UUID;
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
  private final List<Relationship> relationships;

  @AllArgsConstructor
  @Builder
  @Getter
  public static class Relationship {
    private String type;
    private UUID id;
    private JsonApiResource included;
  }
}
