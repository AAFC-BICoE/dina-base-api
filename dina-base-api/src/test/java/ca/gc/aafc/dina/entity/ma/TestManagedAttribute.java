package ca.gc.aafc.dina.entity.ma;

import ca.gc.aafc.dina.entity.ManagedAttribute;
import ca.gc.aafc.dina.i18n.MultilingualDescription;
import ca.gc.aafc.dina.i18n.MultilingualTitle;
import com.vladmihalcea.hibernate.type.array.StringArrayType;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.Builder;
import lombok.Data;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;
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
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
@TypeDef(name = "string-array", typeClass = StringArrayType.class)
public class TestManagedAttribute implements ManagedAttribute {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;
  private UUID uuid;
  private String name;
  private String key;

  @NotNull
  @Enumerated(EnumType.STRING)
  private ManagedAttributeType managedAttributeType;

  @Type(type = "string-array")
  private String[] acceptedValues;

  private String createdBy;
  private OffsetDateTime createdOn;
  //for testing purpose
  private boolean failValidateValue;

  @Type(type = "jsonb")
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
