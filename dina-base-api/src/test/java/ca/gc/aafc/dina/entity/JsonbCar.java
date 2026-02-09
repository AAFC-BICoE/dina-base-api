package ca.gc.aafc.dina.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.NaturalId;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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
public class JsonbCar implements DinaEntity {

  @Id
  private Integer id;
  @NaturalId
  private UUID uuid;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private Map<String, Object> jsonData;

  @JdbcTypeCode(SqlTypes.JSON)
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
