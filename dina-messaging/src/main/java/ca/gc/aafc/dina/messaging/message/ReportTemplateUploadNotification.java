package ca.gc.aafc.dina.messaging.message;

import ca.gc.aafc.dina.messaging.DinaMessage;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonTypeId;

/**
 * Indicates that a report-template is uploaded and can be retrieved using the toa.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportTemplateUploadNotification implements DinaMessage {

  public static final String TYPE = "ReportTemplateUploadNotification";

  @JsonTypeId
  @EqualsAndHashCode.Exclude
  private String type = TYPE;

  // uuid generated for the report-template
  private UUID uuid;

  private String username;

  // temporary object access
  private String toa;

  @Override
  public String getType() {
    return TYPE;
  }
}
