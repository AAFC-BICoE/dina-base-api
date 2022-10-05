package ca.gc.aafc.dina.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

public class DinaAuthenticatedUserTest {

  @Test
  public void authenticatedUser_OnNullGroupsRoles_ReturnEmptySets() {
    DinaAuthenticatedUser user = DinaAuthenticatedUser.builder()
      .username("abc")
      .build();

    assertNotNull(user.getGroups());
    assertNotNull(user.getRolesPerGroup());
    assertNotNull(user.getGroupsForMinimumRole(DinaRole.USER));
  }

  @Test
  public void authenticatedUser_onGetGroupsForMinimumRole_ExpectedResultsReturned() {
    DinaAuthenticatedUser user = DinaAuthenticatedUser.builder()
            .username("abc")
            .rolesPerGroup(
                    Map.of("group1", Set.of(DinaRole.GUEST),
                            "group2", Set.of(DinaRole.USER))
            ).build();

    assertEquals("group2", user.getGroupsForMinimumRole(DinaRole.USER).iterator().next());
    assertEquals(2, user.getGroupsForMinimumRole(DinaRole.GUEST).size());
  }

}