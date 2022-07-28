package ca.gc.aafc.dina.security.spring;

import ca.gc.aafc.dina.entity.Person;
import ca.gc.aafc.dina.security.DinaAuthenticatedUser;
import ca.gc.aafc.dina.security.DinaRole;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.mockito.Answers;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DinaPermissionEvaluatorTest {

  public static final String GROUP_1 = "group1";
  public static final String USERNAME= "name";

  private DinaPermissionEvaluator evaluator;

  @BeforeEach
  void setUp() {
    KeycloakAuthenticationToken mockToken = Mockito.mock(
      KeycloakAuthenticationToken.class, Answers.RETURNS_DEEP_STUBS);
    evaluator = new DinaPermissionEvaluator(mockToken);
  }

  @ParameterizedTest
  @ValueSource(strings = {"super_user", "SUPER_USER", "   SUPER_USER   "})
  void hasDinaRole_hasRole_returnsTrue(String role) {
    DinaAuthenticatedUser user = getDinaAuthenticatedUser(DinaRole.SUPER_USER);
    Assertions.assertTrue(evaluator.hasDinaRole(user, role));
  }

  @ParameterizedTest
  @ValueSource(strings = {"super_user", "SUPER_USER", "   SUPER_USER   "})
  void hasDinaRole_doesNotHaveRole_returnsFalse(String role) {
    DinaAuthenticatedUser user = getDinaAuthenticatedUser(DinaRole.USER);
    Assertions.assertFalse(evaluator.hasDinaRole(user, role));
  }

  @ParameterizedTest
  @NullAndEmptySource
  void hasDinaRole_BlankRole_returnsFalse(String role) {
    DinaAuthenticatedUser user = getDinaAuthenticatedUser(DinaRole.USER);
    Assertions.assertFalse(evaluator.hasDinaRole(user, role));
  }

  @Test
  void hasDinaRole_NullUser_returnsFalse() {
    Assertions.assertFalse(evaluator.hasDinaRole(null, "role"));
  }

  @Test
  void hasDinaRole_UserWithNoRoles_returnsFalse() {
    Assertions.assertFalse(evaluator.hasDinaRole(DinaAuthenticatedUser.builder()
      .rolesPerGroup(Collections.emptyMap())
      .build(), "role"));
    Assertions.assertFalse(evaluator.hasDinaRole(DinaAuthenticatedUser.builder()
      .rolesPerGroup(null)
      .build(), "role"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"group1", "GROUP1", "   group1   "})
  void hasGroupAndRolePermissions_hasRoleAndGroup_returnsTrue(String group) {
    DinaAuthenticatedUser user = getDinaAuthenticatedUser(DinaRole.SUPER_USER);
    Assertions.assertTrue(evaluator.hasGroupAndRolePermissions(user, "super_user",
      Person.builder().group(group).build()));
  }

  @Test
  void hasGroupAndRolePermissions_hasRoleButNoGroup_returnsTrue() {
    DinaAuthenticatedUser user = getDinaAuthenticatedUser(DinaRole.SUPER_USER);
    Assertions.assertFalse(evaluator.hasGroupAndRolePermissions(user, "super_user",
      Person.builder().group("invalid group").build()));
  }

  @ParameterizedTest
  @ValueSource(strings = {"group1", "GROUP1", "   group1   "})
  void hasGroupAndRolePermissions_hasGroupButNoRole_returnsTrue(String group) {
    DinaAuthenticatedUser user = getDinaAuthenticatedUser(DinaRole.SUPER_USER);
    Assertions.assertFalse(evaluator.hasGroupAndRolePermissions(user, DinaRole.USER.toString(),
      Person.builder().group(group).build()));
  }

  @ParameterizedTest
  @NullAndEmptySource
  void hasGroupAndRolePermissions_BlankRole_returnsFalse(String role) {
    DinaAuthenticatedUser user = getDinaAuthenticatedUser(DinaRole.USER);
    Assertions.assertFalse(evaluator.hasGroupAndRolePermissions(user, role,
      Person.builder().group(GROUP_1).build()));
  }

  @ParameterizedTest
  @NullAndEmptySource
  void hasGroupAndRolePermissions_BlankGroup_returnsFalse(String group) {
    DinaAuthenticatedUser user = getDinaAuthenticatedUser(DinaRole.USER);
    Assertions.assertFalse(evaluator.hasGroupAndRolePermissions(user, "user",
      Person.builder().group(group).build()));
  }

  @ParameterizedTest
  @ValueSource(strings = {"group1", "GROUP1", "   group1   "})
  void hasMinimumGroupAndRolePermissions_hasMinimumRoleAndGroup_returnsTrue(String group) {
    DinaAuthenticatedUser user = getDinaAuthenticatedUser(DinaRole.SUPER_USER);
    assertTrue(evaluator.hasMinimumGroupAndRolePermissions(user, "user",
      Person.builder().group(group).build()));
    assertTrue(evaluator.hasMinimumGroupAndRolePermissions(user, "super_user",
      Person.builder().group(group).build()));
    assertFalse(evaluator.hasMinimumGroupAndRolePermissions(user, "DINA_ADMIN",
      Person.builder().group(group).build()));
  }

  @Test
  void hasMinimumGroupAndRolePermissions_hasRoleButNoGroup_returnsTrue() {
    DinaAuthenticatedUser user = getDinaAuthenticatedUser(DinaRole.SUPER_USER);
    Assertions.assertFalse(evaluator.hasMinimumGroupAndRolePermissions(user, "super_user",
      Person.builder().group("invalid group").build()));
  }

  @ParameterizedTest
  @ValueSource(strings = {"group1", "GROUP1", "   group1   "})
  void hasMinimumGroupAndRolePermissions_hasGroupButNoRole_returnsTrue(String group) {
    DinaAuthenticatedUser user = getDinaAuthenticatedUser(DinaRole.GUEST);
    Assertions.assertFalse(evaluator.hasMinimumGroupAndRolePermissions(user, "user",
      Person.builder().group(group).build()));
  }

  @ParameterizedTest
  @NullAndEmptySource
  void hasMinimumGroupAndRolePermissions_BlankRole_returnsFalse(String role) {
    DinaAuthenticatedUser user = getDinaAuthenticatedUser(DinaRole.USER);
    Assertions.assertFalse(evaluator.hasMinimumGroupAndRolePermissions(user, role,
      Person.builder().group(GROUP_1).build()));
  }

  @ParameterizedTest
  @NullAndEmptySource
  void hasMinimumGroupAndRolePermissions_BlankGroup_returnsFalse(String group) {
    DinaAuthenticatedUser user = getDinaAuthenticatedUser(DinaRole.USER);
    Assertions.assertFalse(evaluator.hasMinimumGroupAndRolePermissions(user, "user",
      Person.builder().group(group).build()));
  }

  @Test
  void hasMinimumDinaRole_hasRole_returnsTrue() {
    // Test a user with the exact role.
    DinaAuthenticatedUser exactRoleUser = getDinaAuthenticatedUser(DinaRole.SUPER_USER);
    Assertions.assertTrue(evaluator.hasMinimumDinaRole(exactRoleUser, "super_user"));

    // Test a user with a higher role.
    DinaAuthenticatedUser higherRoleUser = getDinaAuthenticatedUser(DinaRole.DINA_ADMIN);
    Assertions.assertTrue(evaluator.hasMinimumDinaRole(higherRoleUser, "super_user"));
  }

  @Test
  void hasMinimumDinaRole_lowerRole_returnsFalse() {
    // Test a user with a lower role.
    DinaAuthenticatedUser lowerRoleUser = getDinaAuthenticatedUser(DinaRole.GUEST);
    Assertions.assertFalse(evaluator.hasMinimumDinaRole(lowerRoleUser, "super_user"));
  }

  @Test
  void hasMinimumDinaRole_blankRole_returnsFalse() {
    // Since the minimum role was defined as null it will return null.
    DinaAuthenticatedUser user = getDinaAuthenticatedUser(DinaRole.DINA_ADMIN);
    Assertions.assertFalse(evaluator.hasMinimumDinaRole(user, null));
  }

  @Test
  void hasMinimumDinaRole_roleNotFound_returnsFalse() {
    // Since the minimum role could not be found, it should return false.
    DinaAuthenticatedUser user = getDinaAuthenticatedUser(DinaRole.DINA_ADMIN);
    Assertions.assertFalse(evaluator.hasMinimumDinaRole(user, "not_a_real_role"));
  }

  @Test
  void hasMinimumDinaRole_blankUser_returnsFalse() {
    // This should return false since no user was provided.
    assertFalse(evaluator.hasMinimumDinaRole(null, DinaRole.GUEST.toString()));
  }

  @Test
  void hasObjectOwnership_whenObjectNotOwned_returnsFalse() {
    DinaAuthenticatedUser user = getDinaAuthenticatedUser(DinaRole.USER);

    Person p = Person.builder().build();
    assertFalse(evaluator.hasObjectOwnership(user, p));

    p = Person.builder().group(GROUP_1).createdBy("xyz").build();
    assertFalse(evaluator.hasObjectOwnership(user, p));

    assertFalse(evaluator.hasObjectOwnership(user, null));

  }

  @Test
  void hasObjectOwnership_whenObjectOwned_returnsTrue() {
    DinaAuthenticatedUser user = getDinaAuthenticatedUser(DinaRole.USER);
    Person p =  Person.builder().group(GROUP_1).createdBy(USERNAME).build();
    assertTrue(evaluator.hasObjectOwnership(user, p));
  }

  @Test
  void dinaRolePriorityComparison() {
    assertTrue(DinaRole.SUPER_USER.isHigherThan(DinaRole.GUEST));
    assertTrue(DinaRole.SUPER_USER.isHigherOrEqualThan(DinaRole.SUPER_USER));

    assertFalse(DinaRole.SUPER_USER.isHigherOrEqualThan(DinaRole.DINA_ADMIN));
    assertFalse(DinaRole.GUEST.isHigherOrEqualThan(DinaRole.DINA_ADMIN));

    assertEquals(-1, DinaRole.COMPARATOR.compare(DinaRole.SUPER_USER, DinaRole.GUEST));

    // test sorting by priority
    List<DinaRole> dinaRoleList = new ArrayList<>(List.of(DinaRole.GUEST, DinaRole.SUPER_USER, DinaRole.READ_ONLY, DinaRole.DINA_ADMIN));
    dinaRoleList.sort(DinaRole.COMPARATOR);
    assertEquals(List.of(DinaRole.DINA_ADMIN, DinaRole.SUPER_USER, DinaRole.GUEST, DinaRole.READ_ONLY), dinaRoleList);
  }

  private static DinaAuthenticatedUser getDinaAuthenticatedUser(DinaRole dinaRole) {
    return DinaAuthenticatedUser.builder()
      .username(USERNAME)
      .rolesPerGroup(ImmutableMap.of(GROUP_1, ImmutableSet.of(dinaRole)))
      .build();
  }
}
