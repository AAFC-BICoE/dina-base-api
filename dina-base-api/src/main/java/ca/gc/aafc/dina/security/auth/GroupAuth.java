package ca.gc.aafc.dina.security.auth;

import java.time.OffsetDateTime;
import java.util.UUID;

import ca.gc.aafc.dina.entity.DinaEntity;

/**
 * Record that represents a group only.
 * It implements DinaEntity so it can be used with {@link GroupAuthorizationService}.
 * @param group
 */
public record GroupAuth(String group) implements DinaEntity {

  /**
   * Build a new {@link GroupAuth} instance for the provided group.
   * @param group
   * @return
   */
  public static GroupAuth of(String group) {
    return new GroupAuth(group);
  }

  @Override
  public Integer getId() {
    return null;
  }

  @Override
  public UUID getUuid() {
    return null;
  }

  /**
   * We need to return the group using getGroup to follow Java Bean convention.
   * @return
   */
  @Override
  public String getGroup() {
    return group;
  }

  @Override
  public String getCreatedBy() {
    return null;
  }

  @Override
  public OffsetDateTime getCreatedOn() {
    return null;
  }

}
