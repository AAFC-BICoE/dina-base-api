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
 * Notification message for data export events.
 * Only triggered when required (e.g. we need to store the generated export in the object-store).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataExportNotification implements DinaMessage {

  public static final String TYPE = "DataExportNotification";

  @JsonTypeId
  @EqualsAndHashCode.Exclude
  @Builder.Default
  private String type = TYPE;

  // uuid generated for the export
  private UUID uuid;

  private String username;
  private String group;

  // name of the export (user provided)
  private String name;

  // should we persist the export in the object-store
  private Boolean persistExport;

  // temporary object access
  private String toa;

  @Override
  public String getType() {
    return TYPE;
  }
}
