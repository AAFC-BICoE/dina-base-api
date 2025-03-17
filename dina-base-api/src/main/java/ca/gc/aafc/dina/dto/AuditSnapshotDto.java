package ca.gc.aafc.dina.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.toedter.spring.hateoas.jsonapi.JsonApiId;
import com.toedter.spring.hateoas.jsonapi.JsonApiTypeForClass;

@JsonApiTypeForClass(AuditSnapshotDto.TYPE_NAME)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditSnapshotDto {

  public static final String TYPE_NAME = "audit-snapshot";

  @JsonApiId
  private String id;

  /**
   * The audited record's type and ID.
   * e.g. metadata/16b3a97c-5485-4920-891d-e0709f3b22d4
   */
  private String instanceId;

  /** Snapshot state. */
  private Map<String, Object> state;

  private List<String> changedProperties;

  /** INITIAL / UPDATE / TERMINAL */
  private String snapshotType;

  private Long version;

  /** Who made the change. */
  private String author;

  private OffsetDateTime commitDateTime;

}
