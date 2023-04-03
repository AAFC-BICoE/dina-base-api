package ca.gc.aafc.dina.entity;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Summary that contains information to perform authorization without loading the entire entity.
 */
@AllArgsConstructor
@Getter
public class AuthorizationSummary {
  private UUID uuid;
  private String group;
  private String createdBy;
}
