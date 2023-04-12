package ca.gc.aafc.dina.jpa;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Summary that contains information to perform actions (e.g. authorization) without loading the entire entity.
 */
@AllArgsConstructor
@Getter
public class DinaObjectSummary {
  private UUID uuid;
  private String group;
  private String createdBy;
}
