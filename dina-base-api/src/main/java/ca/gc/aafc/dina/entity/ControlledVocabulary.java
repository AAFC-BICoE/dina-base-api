package ca.gc.aafc.dina.entity;

import java.time.OffsetDateTime;
import java.util.UUID;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
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

  @Type(type = "pgsql_enum")
  @Enumerated(EnumType.STRING)
  @NotNull
  private ControlledVocabularyType type;

  @Type(type = "pgsql_enum")
  @Enumerated(EnumType.STRING)
  @NotNull
  private ControlledVocabularyClass vocabClass;

  /**
   * Immutable and stable key representing the vocabulary.
   * Used as stable identifier within the system boundaries.
   *
   * The key is mandatory and can't contain a dot (.).
   *
   */
  @NotBlank
  @Pattern(regexp = "^[^.]+$")
  @Size(max = 255)
  private String key;

  @Size(max = 255)
  private String version;

  @Type(type = "jsonb")
  private MultilingualTitle multilingualTitle;

  @Type(type = "jsonb")
  private MultilingualDescription multilingualDescription;

  private String createdBy;

  private OffsetDateTime createdOn;

}
