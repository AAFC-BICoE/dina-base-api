package ca.gc.aafc.dina.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import ca.gc.aafc.dina.i18n.MultilingualDescription;
import ca.gc.aafc.dina.i18n.MultilingualTitle;
import ca.gc.aafc.dina.vocabulary.TypedVocabularyElement;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Data;

@Data
public abstract class BaseControlledVocabularyItemDto implements JsonApiResource {

  public static final String TYPENAME = "controlled-vocabulary-item";

  protected UUID uuid;

  protected String name;
  protected String key;
  protected String term;

  protected MultilingualTitle multilingualTitle;
  protected MultilingualDescription multilingualDescription;

  protected TypedVocabularyElement.VocabularyElementType vocabularyElementType;
  protected String[] acceptedValues;

  protected String unit;

  protected String dinaComponent;

  protected String createdBy;
  protected OffsetDateTime createdOn;

  public abstract <T extends BaseControlledVocabularyDto> T getControlledVocabulary();

  @Override
  @JsonIgnore
  public String getJsonApiType() {
    return TYPENAME;
  }

  @Override
  @JsonIgnore
  public UUID getJsonApiId() {
    return uuid;
  }
}
