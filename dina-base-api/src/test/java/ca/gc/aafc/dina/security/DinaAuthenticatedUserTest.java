package ca.gc.aafc.dina.security;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class DinaAuthenticatedUserTest {

  @Test
  public void authenticatedUser_OnNullGroupsRoles_ReturnEmptySets() {
    DinaAuthenticatedUser user = DinaAuthenticatedUser.builder()
      .username("abc")
      .build();

    assertNotNull(user.getGroups());
    assertNotNull(user.getRolesPerGroup());
  }

}