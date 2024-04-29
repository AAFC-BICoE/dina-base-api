package ca.gc.aafc.dina.messaging;

import ca.gc.aafc.dina.messaging.message.DocumentOperationType;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.UUID;

/**
 * Event representing a change to an entity
 */
@RequiredArgsConstructor
@Builder
@Getter
@ToString
public class EntityChanged {
  private final DocumentOperationType op;
  private final UUID uuid;
  private final String resourceType;
}
