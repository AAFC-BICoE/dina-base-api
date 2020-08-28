package ca.gc.aafc.dina.dto;

import ca.gc.aafc.dina.entity.Person;
import ca.gc.aafc.dina.entity.Vocabulary;
import io.crnk.core.resource.annotations.JsonApiId;
import io.crnk.core.resource.annotations.JsonApiResource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@JsonApiResource(type = VocabularyDto.TYPE_NAME)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RelatedEntity(Vocabulary.class)
public class VocabularyDto {

  public static final String TYPE_NAME = "vocabulary";

  @JsonApiId
  private UUID uuid;

  private String name;
}
