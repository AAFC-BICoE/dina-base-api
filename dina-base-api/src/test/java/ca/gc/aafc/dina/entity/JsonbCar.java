package ca.gc.aafc.dina.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.Type;

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

  @Type(JsonType.class)
  @Column(columnDefinition = "jsonb")
  private Map<String, Object> jsonData;

  @Type(JsonType.class)
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
