package ca.gc.aafc.dina.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import ca.gc.aafc.dina.entity.ControlledVocabulary;
import ca.gc.aafc.dina.i18n.MultilingualDescription;
import ca.gc.aafc.dina.i18n.MultilingualTitle;

import java.time.OffsetDateTime;
import java.util.UUID;

public abstract class BaseControlledVocabularyDto implements JsonApiResource {

  public static final String TYPENAME = "managed-attribute";

  private UUID uuid;
  private String name;
  private String key;

  private ControlledVocabulary.ControlledVocabularyType type;
  private ControlledVocabulary.ControlledVocabularyClass vocabClass;

  private String term;
  private String version;
  private MultilingualTitle multilingualTitle;
  private MultilingualDescription multilingualDescription;
  private String createdBy;
  private OffsetDateTime createdOn;

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
