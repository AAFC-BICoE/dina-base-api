package ca.gc.aafc.dina.entity.ma;

import ca.gc.aafc.dina.entity.ManagedAttribute;
import ca.gc.aafc.dina.i18n.MultilingualDescription;
import ca.gc.aafc.dina.i18n.MultilingualTitle;
import ca.gc.aafc.dina.vocabulary.TypedVocabularyElement;

import lombok.Builder;
import lombok.Data;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Test implementation of a {@link ManagedAttribute}.
 * Since it's running on H2 the uniqueness if not really define so the test will assume
 * it is by key or key/component depending on the purpose of the test.
 */
@Data
@Builder
@Entity
public class TestManagedAttribute implements ManagedAttribute {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;
  private UUID uuid;
  private String name;
  private String key;
  private String unit;

  @NotNull
  @Enumerated(EnumType.STRING)
  private TypedVocabularyElement.VocabularyElementType vocabularyElementType;

  //@Type(type = "string-array")
  private String[] acceptedValues;

  private String createdBy;
  private OffsetDateTime createdOn;
  //for testing purpose
  private boolean failValidateValue;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private MultilingualDescription multilingualDescription;

  // matches XYZValidationContext toString
  private String component;

  @Override
  public String getTerm() {
    return null;
  }

  @Override
  public MultilingualTitle getMultilingualTitle() {
    return null;
  }
}
