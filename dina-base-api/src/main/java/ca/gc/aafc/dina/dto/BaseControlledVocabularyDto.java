package ca.gc.aafc.dina.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import ca.gc.aafc.dina.entity.ControlledVocabulary;
import ca.gc.aafc.dina.i18n.MultilingualDescription;
import ca.gc.aafc.dina.i18n.MultilingualTitle;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Data;

@Data
public abstract class BaseControlledVocabularyDto implements JsonApiResource {

  public static final String TYPENAME = "controlled-vocabulary";

  protected UUID uuid;
  protected String name;
  protected String key;

  protected ControlledVocabulary.ControlledVocabularyType type;
  protected ControlledVocabulary.ControlledVocabularyClass vocabClass;

  protected String term;
  protected String version;
  protected MultilingualTitle multilingualTitle;
  protected MultilingualDescription multilingualDescription;
  protected String createdBy;
  protected OffsetDateTime createdOn;

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
