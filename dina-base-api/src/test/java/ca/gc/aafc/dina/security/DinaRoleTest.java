package ca.gc.aafc.dina.security;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DinaRoleTest {

  @Test
  public void testFromString() {

    assertEquals(DinaRole.SUPER_USER, DinaRole.fromString("super-user").orElse(null));

    // test with a space
    assertEquals(Optional.empty(), DinaRole.fromString(" dina-admin"));

    assertEquals(Optional.empty(), DinaRole.fromString(null));
  }

  @Test
  public void testAdminAndGroupBased() {

    assertTrue(DinaRole.adminBasedRoles().contains(DinaRole.DINA_ADMIN));
    assertFalse(DinaRole.adminBasedRoles().contains(DinaRole.SUPER_USER));

    assertTrue(DinaRole.groupBasedRoles().contains(DinaRole.SUPER_USER));
    assertFalse(DinaRole.groupBasedRoles().contains(DinaRole.DINA_ADMIN));

    //make sure all groups are covered
    List<DinaRole> allRoles = Arrays.stream(DinaRole.values()).collect(Collectors.toList());
    allRoles.removeAll(DinaRole.adminBasedRoles());
    allRoles.removeAll(DinaRole.groupBasedRoles());
    assertEquals(0, allRoles.size());
  }

}
