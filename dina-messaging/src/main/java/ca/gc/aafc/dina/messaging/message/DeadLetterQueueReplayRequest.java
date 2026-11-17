package ca.gc.aafc.dina.messaging.message;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonTypeId;

import ca.gc.aafc.dina.messaging.DinaMessage;

/**
 * Requests that a consumer replay the current contents of its dead-letter queue.
 */
@Data
@NoArgsConstructor
public class DeadLetterQueueReplayRequest implements DinaMessage {

  public static final String TYPE = "DeadLetterQueueReplayRequest";

  @JsonTypeId
  @EqualsAndHashCode.Exclude
  private String type = TYPE;

  @Override
  public String getType() {
    return TYPE;
  }
}
