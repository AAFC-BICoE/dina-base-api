package ca.gc.aafc.dina.entity;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Test class with jsonb-based field.
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class JsonbCar implements DinaEntity {

  @Id
  private Integer id;
  @NaturalId
  private UUID uuid;

  @Type(type = "jsonb")
  @Column(columnDefinition = "jsonb")
  private Map<String, Object> jsonData;

  @Type(type = "jsonb")
  @Column(columnDefinition = "jsonb")
  private List<CarDetails> jsonListData;

  private String createdBy;
  private OffsetDateTime createdOn;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CarDetails {
    private String value;
  }
}
