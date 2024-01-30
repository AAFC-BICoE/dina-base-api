package ca.gc.aafc.dina.messaging.message;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import ca.gc.aafc.dina.messaging.DinaMessage;

@Builder
@AllArgsConstructor
@Getter
public class ObjectExportNotification implements DinaMessage {

  // uuid generated for the export
  private UUID uuid;

  private String username;

  // temporary object access
  private String toa;
  
}
