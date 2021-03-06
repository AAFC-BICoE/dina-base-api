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

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DinaPermissionEvaluatorTest {

  public static final String GROUP_1 = "group1";
  private DinaPermissionEvaluator evaluator;

  @BeforeEach
  void setUp() {
    KeycloakAuthenticationToken mockToken = Mockito.mock(
      KeycloakAuthenticationToken.class,
      Answers.RETURNS_DEEP_STUBS);
    evaluator = new DinaPermissionEvaluator(mockToken);
  }

  @ParameterizedTest
  @ValueSource(strings = {"collection_manager", "COLLECTION_MANAGER", "   COLLECTION_MANAGER   "})
  void hasDinaRole_hasRole_returnsTrue(String role) {
    DinaAuthenticatedUser user = getDinaAuthenticatedUser(DinaRole.COLLECTION_MANAGER);
    Assertions.assertTrue(evaluator.hasDinaRole(user, role));
  }

  @ParameterizedTest
  @ValueSource(strings = {"collection_manager", "COLLECTION_MANAGER", "   COLLECTION_MANAGER   "})
  void hasDinaRole_doesNotHaveRole_returnsFalse(String role) {
    DinaAuthenticatedUser user = getDinaAuthenticatedUser(DinaRole.STAFF);
    Assertions.assertFalse(evaluator.hasDinaRole(user, role));
  }

  @ParameterizedTest
  @NullAndEmptySource
  void hasDinaRole_BlankRole_returnsFalse(String role) {
    DinaAuthenticatedUser user = getDinaAuthenticatedUser(DinaRole.STAFF);
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
    DinaAuthenticatedUser user = getDinaAuthenticatedUser(DinaRole.COLLECTION_MANAGER);
    Assertions.assertTrue(evaluator.hasGroupAndRolePermissions(user, "collection_manager",
      Person.builder().group(group).build()));
  }

  @Test
  void hasGroupAndRolePermissions_hasRoleButNoGroup_returnsTrue() {
    DinaAuthenticatedUser user = getDinaAuthenticatedUser(DinaRole.COLLECTION_MANAGER);
    Assertions.assertFalse(evaluator.hasGroupAndRolePermissions(user, "collection_manager",
      Person.builder().group("invalid group").build()));
  }

  @ParameterizedTest
  @ValueSource(strings = {"group1", "GROUP1", "   group1   "})
  void hasGroupAndRolePermissions_hasGroupButNoRole_returnsTrue(String group) {
    DinaAuthenticatedUser user = getDinaAuthenticatedUser(DinaRole.COLLECTION_MANAGER);
    Assertions.assertFalse(evaluator.hasGroupAndRolePermissions(user, "staff",
      Person.builder().group(group).build()));
  }

  @ParameterizedTest
  @NullAndEmptySource
  void hasGroupAndRolePermissions_BlankRole_returnsFalse(String role) {
    DinaAuthenticatedUser user = getDinaAuthenticatedUser(DinaRole.STAFF);
    Assertions.assertFalse(evaluator.hasGroupAndRolePermissions(user, role,
      Person.builder().group(GROUP_1).build()));
  }

  @ParameterizedTest
  @NullAndEmptySource
  void hasGroupAndRolePermissions_BlankGroup_returnsFalse(String group) {
    DinaAuthenticatedUser user = getDinaAuthenticatedUser(DinaRole.STAFF);
    Assertions.assertFalse(evaluator.hasGroupAndRolePermissions(user, "staff",
      Person.builder().group(group).build()));
  }

  @ParameterizedTest
  @ValueSource(strings = {"group1", "GROUP1", "   group1   "})
  void hasMinimumGroupAndRolePermissions_hasMinimumRoleAndGroup_returnsTrue(String group) {
    DinaAuthenticatedUser user = getDinaAuthenticatedUser(DinaRole.COLLECTION_MANAGER);
    Assertions.assertTrue(evaluator.hasMinimumGroupAndRolePermissions(user, "staff",
      Person.builder().group(group).build()));
    Assertions.assertTrue(evaluator.hasMinimumGroupAndRolePermissions(user, "collection_manager",
      Person.builder().group(group).build()));
    Assertions.assertFalse(evaluator.hasMinimumGroupAndRolePermissions(user, "DINA_ADMIN",
      Person.builder().group(group).build()));
  }

  @Test
  void hasMinimumGroupAndRolePermissions_hasRoleButNoGroup_returnsTrue() {
    DinaAuthenticatedUser user = getDinaAuthenticatedUser(DinaRole.COLLECTION_MANAGER);
    Assertions.assertFalse(evaluator.hasMinimumGroupAndRolePermissions(user, "collection_manager",
      Person.builder().group("invalid group").build()));
  }

  @ParameterizedTest
  @ValueSource(strings = {"group1", "GROUP1", "   group1   "})
  void hasMinimumGroupAndRolePermissions_hasGroupButNoRole_returnsTrue(String group) {
    DinaAuthenticatedUser user = getDinaAuthenticatedUser(DinaRole.STUDENT);
    Assertions.assertFalse(evaluator.hasMinimumGroupAndRolePermissions(user, "staff",
      Person.builder().group(group).build()));
  }

  @ParameterizedTest
  @NullAndEmptySource
  void hasMinimumGroupAndRolePermissions_BlankRole_returnsFalse(String role) {
    DinaAuthenticatedUser user = getDinaAuthenticatedUser(DinaRole.STAFF);
    Assertions.assertFalse(evaluator.hasMinimumGroupAndRolePermissions(user, role,
      Person.builder().group(GROUP_1).build()));
  }

  @ParameterizedTest
  @NullAndEmptySource
  void hasMinimumGroupAndRolePermissions_BlankGroup_returnsFalse(String group) {
    DinaAuthenticatedUser user = getDinaAuthenticatedUser(DinaRole.STAFF);
    Assertions.assertFalse(evaluator.hasMinimumGroupAndRolePermissions(user, "staff",
      Person.builder().group(group).build()));
  }

  @Test
  void dinaRolePriorityComparison() {

    assertTrue(DinaRole.COLLECTION_MANAGER.isHigherThan(DinaRole.STUDENT));
    assertTrue(DinaRole.COLLECTION_MANAGER.isHigherOrEqualThan(DinaRole.COLLECTION_MANAGER));

    assertFalse(DinaRole.COLLECTION_MANAGER.isHigherOrEqualThan(DinaRole.DINA_ADMIN));
    assertFalse(DinaRole.STUDENT.isHigherOrEqualThan(DinaRole.DINA_ADMIN));
  }

  private static DinaAuthenticatedUser getDinaAuthenticatedUser(DinaRole dinaRole) {
    return DinaAuthenticatedUser.builder()
      .username("name")
      .rolesPerGroup(ImmutableMap.of(GROUP_1, ImmutableSet.of(dinaRole)))
      .build();
  }
}
