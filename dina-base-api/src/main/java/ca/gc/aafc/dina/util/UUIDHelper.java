package ca.gc.aafc.dina.util;

import java.util.UUID;

import com.fasterxml.uuid.Generators;

/**
 * Helper class to handle UUID version 7.
 */
public final class UUIDHelper {

  private UUIDHelper() {
    // utility class
  }

  public static UUID generateUUIDv7() {
   return Generators.timeBasedEpochGenerator().generate();
  }

  /**
   * Checks if the provided UUID is of version 7.
   * @param uuid
   * @return is provided UUID version 7. If uuid is null false is returned.
   */
  public static boolean isUUIDv7(UUID uuid) {
    if(uuid == null) {
      return false;
    }
    return uuid.version() == 7;
  }

}
