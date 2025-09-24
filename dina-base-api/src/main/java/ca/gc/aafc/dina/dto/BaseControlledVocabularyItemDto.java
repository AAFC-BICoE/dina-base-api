package ca.gc.aafc.dina.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import ca.gc.aafc.dina.i18n.MultilingualDescription;
import ca.gc.aafc.dina.i18n.MultilingualTitle;
import ca.gc.aafc.dina.vocabulary.TypedVocabularyElement;

import java.time.OffsetDateTime;
import java.util.UUID;

public abstract class BaseControlledVocabularyItemDto implements JsonApiResource {

  public static final String TYPENAME = "managed-attribute-item";

  private UUID uuid;

  private String name;
  private String key;
  private String term;

  private MultilingualTitle multilingualTitle;
  private MultilingualDescription multilingualDescription;

  private TypedVocabularyElement.VocabularyElementType vocabularyElementType;
  private String[] acceptedValues;

  private String unit;

  private String dinaComponent;
  
  private String createdBy;
  private OffsetDateTime createdOn;

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
