package ca.gc.aafc.dina.messaging.message;

import ca.gc.aafc.dina.messaging.DinaMessage;

import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Builder
@Data
@Getter
public class ObjectExportNotification extends DinaMessage {

  public static final String TYPE = "ObjectExportNotification";

  public ObjectExportNotification() {
    super(TYPE);
  }

  public ObjectExportNotification(UUID uuid, String username, String name, String query, String toa) {
    super(TYPE);
    this.uuid = uuid;
    this.username = username;
    this.name = name;
    this.query = query;
    this.toa = toa;
  }

  // uuid generated for the export
  private UUID uuid;

  private String username;

  // name of the export (user provided)
  private String name;

  private String query;

  // temporary object access
  private String toa;

}
