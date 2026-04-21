package ca.gc.aafc.dina.entity;

import io.hypersistence.utils.hibernate.type.basic.PostgreSQLEnumType;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import java.time.OffsetDateTime;
import java.util.UUID;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;


import org.hibernate.annotations.JdbcTypeCode;

import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NaturalIdCache;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import ca.gc.aafc.dina.i18n.MultilingualDescription;
import ca.gc.aafc.dina.i18n.MultilingualTitle;

@MappedSuperclass
@Getter
@Setter
@AllArgsConstructor
@SuperBuilder
@RequiredArgsConstructor
@NaturalIdCache
public abstract class ControlledVocabulary implements DinaEntity {

  public static final String KEY_ATTRIBUTE_NAME = "key";

  public enum ControlledVocabularyType { MANAGED_ATTRIBUTE, SYSTEM }

  // QUALIFIED_VALUE: Defined set of named term (URI). In its usage, a typed literal is stored with the term. (e.g., pH: 7.2, depth: 12.5 m).
  // CONTROLLED_TERM: Defined set of named term (URI). In its usage there is no separate literal value.; Numbers (if any) are labels. (e.g., coordinateSystem: EPSG:4326; species: Homo sapiens; riskLevel: low/medium/high).
  public enum ControlledVocabularyClass { QUALIFIED_VALUE, CONTROLLED_TERM }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @NaturalId
  private UUID uuid;

  @NotBlank
  @Size(max = 255)
  private String name;

  @Enumerated(EnumType.STRING)
  @Type(PostgreSQLEnumType.class)
  @Column(columnDefinition = "controlled_vocabulary_type")
  @NotNull
  private ControlledVocabularyType type;

  @Enumerated(EnumType.STRING)
  @Type(PostgreSQLEnumType.class)
  @Column(columnDefinition = "controlled_vocabulary_class")
  @NotNull
  private ControlledVocabularyClass vocabClass;

  /**
   * Immutable and stable key representing the vocabulary.
   * Used as stable identifier within the system boundaries.
   * The key is mandatory and can't contain a dot (.).
   */
  @NotBlank
  @Pattern(regexp = "^[^.]+$")
  @Size(max = 255)
  private String key;

  @Size(max = 255)
  private String term;

  @Size(max = 255)
  private String version;

  @Type(JsonType.class)
  private MultilingualTitle multilingualTitle;

  @Type(JsonType.class)
  private MultilingualDescription multilingualDescription;

  @NotBlank
  @Column(name = "created_by", updatable = false)
  @Size(max = 255)
  private String createdBy;

  @Column(name = "created_on", insertable = false, updatable = false)
  @Generated(value = GenerationTime.INSERT)
  private OffsetDateTime createdOn;

}
