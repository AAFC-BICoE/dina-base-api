package ca.gc.aafc.dina.security;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DinaRoleTest {

  @Test
  public void testFromString() {

    assertEquals(DinaRole.COLLECTION_MANAGER, DinaRole.fromString("collection-manager").orElse(null));

    // test with a space
    assertEquals(Optional.empty(), DinaRole.fromString(" dina-admin"));

    assertEquals(Optional.empty(), DinaRole.fromString(null));
  }

}
