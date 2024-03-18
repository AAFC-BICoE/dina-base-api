package ca.gc.aafc.dina.messaging.message;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import ca.gc.aafc.dina.messaging.DinaMessage;

@Builder
@AllArgsConstructor
@Data
@NoArgsConstructor
@Getter
public class ObjectExportNotification implements DinaMessage {

  // uuid generated for the export
  private UUID uuid;

  private String username;

  // name of the export (user provided)
  private String name;

  // temporary object access
  private String toa;
  
}
