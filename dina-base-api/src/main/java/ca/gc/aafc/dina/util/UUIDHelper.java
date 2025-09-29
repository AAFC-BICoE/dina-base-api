package ca.gc.aafc.dina.util;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;

/**
 * Helper class to handle UUID version 7.
 */
public final class UUIDHelper {

  private static final TimeBasedEpochGenerator GENERATOR = Generators.timeBasedEpochGenerator();

  private UUIDHelper() {
    // utility class
  }

  /**
   * thread-safe per TimeBasedEpochGenerator implementation.
   * @return
   */
  public static UUID generateUUIDv7() {
    return GENERATOR.generate();
  }

  /**
   * Checks if the provided UUID is of version 7.
   * @param uuid
   * @return is provided UUID version 7. If uuid is null false is returned.
   */
  public static boolean isUUIDv7(UUID uuid) {
    Objects.requireNonNull(uuid, "UUID must not be null");
    return uuid.version() == 7;
  }

  /**
   * Tries to convert the string argument to uuid.
   * If not possible {@link Optional#empty()} is returned. No exception is thrown.
   * @param possibleUUID
   * @return
   */
  public static Optional<UUID> toUUID(String possibleUUID) {
    if (possibleUUID == null || possibleUUID.length() != 36) {
      return Optional.empty();
    }
    try {
      return Optional.of(UUID.fromString(possibleUUID));
    } catch (IllegalArgumentException iaEx) {
      return Optional.empty();
    }
  }
}
