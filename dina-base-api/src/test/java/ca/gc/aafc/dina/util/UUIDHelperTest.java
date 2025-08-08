package ca.gc.aafc.dina.util;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UUIDHelperTest {

  @Test
  public void testUUIDHelper() {
    UUID uuidv7 = UUIDHelper.generateUUIDv7();

    assertTrue(UUIDHelper.isUUIDv7(uuidv7));
    assertFalse(UUIDHelper.isUUIDv7(UUID.randomUUID()));
  }
}
