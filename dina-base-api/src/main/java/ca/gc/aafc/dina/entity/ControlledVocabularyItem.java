package ca.gc.aafc.dina.entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NaturalIdCache;
import org.hibernate.type.SqlTypes;

import ca.gc.aafc.dina.i18n.MultilingualDescription;
import ca.gc.aafc.dina.i18n.MultilingualTitle;
import ca.gc.aafc.dina.vocabulary.TypedVocabularyElement;

import java.time.OffsetDateTime;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@MappedSuperclass
@Getter
@Setter
@AllArgsConstructor
@SuperBuilder
@RequiredArgsConstructor
@NaturalIdCache
public abstract class ControlledVocabularyItem implements DinaEntity {

  public static final String KEY_ATTRIBUTE_NAME = "key";
  public static final String DINA_COMPONENT_NAME = "dinaComponent";
  public static final String CONTROLLED_VOCABULARY_COL_NAME = "controlled_vocabulary_id";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @NaturalId
  private UUID uuid;

  @NotBlank
  @Size(max = 255)
  private String name;

  @NotBlank
  @Size(max = 50)
  @Column(name = "_group")
  private String group;

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

  @Size(max = 255)
  private String term;

  @JdbcTypeCode(SqlTypes.JSON)
  private MultilingualTitle multilingualTitle;

  @JdbcTypeCode(SqlTypes.JSON)
  private MultilingualDescription multilingualDescription;

  @Enumerated(EnumType.STRING)
  private TypedVocabularyElement.VocabularyElementType vocabularyElementType;


  /**
   * Like wikidata. A URI template where "$1" can be automatically replaced with the value
   * assigned to the identifier.
   */
  private String uriTemplate;

  @JdbcTypeCode(SqlTypes.ARRAY)
  private String[] acceptedValues;

  @Size(max = 50)
  private String unit;

  @Size(max = 250)
  private String dinaComponent;

  @NotBlank
  @Column(name = "created_by", updatable = false)
  @Size(max = 255)
  private String createdBy;

  @Column(name = "created_on", insertable = false, updatable = false)
  @Generated(value = GenerationTime.INSERT)
  private OffsetDateTime createdOn;

  public abstract <T extends ControlledVocabulary> T getControlledVocabulary();
}
