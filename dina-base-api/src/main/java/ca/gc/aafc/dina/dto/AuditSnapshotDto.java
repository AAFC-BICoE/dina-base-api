package ca.gc.aafc.dina.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.crnk.core.resource.annotations.JsonApiId;
import io.crnk.core.resource.annotations.JsonApiResource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonApiResource(type = AuditSnapshotDto.TYPE_NAME)
@Data
@SuppressFBWarnings({ "EI_EXPOSE_REP", "EI_EXPOSE_REP2" })
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
