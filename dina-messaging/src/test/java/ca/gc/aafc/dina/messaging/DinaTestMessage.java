package ca.gc.aafc.dina.messaging;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DinaTestMessage implements DinaMessage {

  public static final String TYPE =  "DinaTestMessage";
  private String name;
  private OffsetDateTime createdOn;

  @Override
  public String getType() {
    return TYPE;
  }
}
