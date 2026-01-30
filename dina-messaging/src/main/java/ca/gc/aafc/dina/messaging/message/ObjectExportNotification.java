package ca.gc.aafc.dina.messaging.message;

import ca.gc.aafc.dina.messaging.DinaMessage;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonTypeId;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ObjectExportNotification implements DinaMessage {

  public static final String TYPE = "ObjectExportNotification";

  @JsonTypeId
  @EqualsAndHashCode.Exclude
  private String type = TYPE;

  // uuid generated for the export
  private UUID uuid;

  private String username;

  // name of the export (user provided)
  private String name;

  // temporary object access
  private String toa;

  @Override
  public String getType() {
    return TYPE;
  }
}
