package ca.gc.aafc.dina.messaging.message;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import ca.gc.aafc.dina.messaging.DinaMessage;

/**
 * Indicates that a report-template is uploaded and can be retrieved using the toa.
 */
@Builder
@AllArgsConstructor
@Data
@NoArgsConstructor
@Getter
public class ReportTemplateUploadNotification implements DinaMessage {

  // uuid generated for the report-template
  private UUID uuid;

  private String username;

  // temporary object access
  private String toa;

}
