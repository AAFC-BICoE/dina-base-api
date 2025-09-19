package ca.gc.aafc.dina.entity;

import java.time.OffsetDateTime;
import java.util.UUID;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NaturalIdCache;
import org.hibernate.annotations.Type;

import ca.gc.aafc.dina.i18n.MultilingualDescription;
import ca.gc.aafc.dina.i18n.MultilingualTitle;
import ca.gc.aafc.dina.vocabulary.TypedVocabularyElement;

@MappedSuperclass
@Getter
@Setter
@AllArgsConstructor
@SuperBuilder
@RequiredArgsConstructor
@NaturalIdCache
public abstract class ControlledVocabularyItem implements DinaEntity {

  public static final String KEY_ATTRIBUTE_NAME = "key";
  public static final String TYPE_COLUMN_NAME = "controlled_vocabulary_id";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @NaturalId
  private UUID uuid;

  @Size(max = 255)
  private String name;

  /**
   * Immutable and stable key representing the vocabulary.
   * Used as stable identifier within the system boundaries.
   * The key is mandatory and can't contain a dot (.).
   *
   */
  @NotBlank
  @Pattern(regexp = "^[^.]+$")
  @Size(max = 255)
  private String key;

  @Type(type = "jsonb")
  private MultilingualTitle multilingualTitle;

  @Type(type = "jsonb")
  private MultilingualDescription multilingualDescription;

  @Enumerated(EnumType.STRING)
  private TypedVocabularyElement.VocabularyElementType vocabularyElementType;

  @Type(type = "string-array")
  private String[] acceptedValues;

  @Size(max = 50)
  private String unit;

  @Size(max = 250)
  private String dinaComponent;

  @Size(max = 255)
  private String createdBy;

  private OffsetDateTime createdOn;

  public abstract <T extends ControlledVocabulary> T getControlledVocabulary();
}
