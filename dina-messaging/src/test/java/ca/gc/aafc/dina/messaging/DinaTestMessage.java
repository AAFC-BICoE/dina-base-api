package ca.gc.aafc.dina.messaging;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class DinaTestMessage extends DinaMessage {

  public static final String TYPE =  "DinaTestMessage";

  private String name;

  public DinaTestMessage() {
    super(TYPE);
  }

  public DinaTestMessage(String name) {
    super(TYPE);
    this.name = name;
  }
}
