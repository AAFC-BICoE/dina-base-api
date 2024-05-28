package ca.gc.aafc.dina.messaging.message;

import ca.gc.aafc.dina.messaging.DinaMessage;

import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

/**
 * Indicates that a report-template is uploaded and can be retrieved using the toa.
 */
@Builder
@Data
@Getter
public class ReportTemplateUploadNotification extends DinaMessage {

  public static final String TYPE = "ReportTemplateUploadNotification";

  public ReportTemplateUploadNotification() {
    super(TYPE);
  }

  public ReportTemplateUploadNotification(UUID uuid, String username, String toa) {
    super(TYPE);
    this.uuid = uuid;
    this.username = username;
    this.toa = toa;
  }

  // uuid generated for the report-template
  private UUID uuid;

  private String username;

  // temporary object access
  private String toa;

}
