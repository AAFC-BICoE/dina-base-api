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
import java.util.Map;
import java.util.UUID;

/**
 * Test class with jsonb-based field, but the annotations are on the method level instead of the
 * field.
 */
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class JsonbMethod implements DinaEntity {
  private Integer id;
  private UUID uuid;
  private Map<String, Object> jsonData;
  private String createdBy;
  private OffsetDateTime createdOn;

  @Id
  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  @NaturalId
  public UUID getUuid() {
    return uuid;
  }

  public void setUuid(UUID uuid) {
    this.uuid = uuid;
  }

  @Type(type = "jsonb")
  @Column(columnDefinition = "jsonb")
  public Map<String, Object> getJsonData() {
    return jsonData;
  }

  public void setJsonData(Map<String, Object> jsonData) {
    this.jsonData = jsonData;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  public OffsetDateTime getCreatedOn() {
    return createdOn;
  }

  public void setCreatedOn(OffsetDateTime createdOn) {
    this.createdOn = createdOn;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CarDetails {
    private String value;
  }
}
