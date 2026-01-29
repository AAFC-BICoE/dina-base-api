package ca.gc.aafc.dina.messaging.message;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonTypeId;
import ca.gc.aafc.dina.messaging.DinaMessage;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserMessageNotification implements DinaMessage {

  public static final String TYPE = "UserNotification";

  @JsonTypeId
  private String type = TYPE;

  private UUID userIdentifier;

  private String group;
  private String notificationType;
  private String title;
  private String message;

  private Map<String, List<MessageParam>> messageParams;
  private OffsetDateTime expiresOn;

  @Override
  public String getType() {
    return TYPE;
  }
}
