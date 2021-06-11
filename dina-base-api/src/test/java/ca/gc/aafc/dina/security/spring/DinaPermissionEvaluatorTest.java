package ca.gc.aafc.dina.security.spring;

import ca.gc.aafc.dina.security.DinaAuthenticatedUser;
import ca.gc.aafc.dina.security.DinaRole;
import ca.gc.aafc.dina.security.PriorityComparator;

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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class DinaPermissionEvaluatorTest {

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

  @Test
  void dinaRolePriorityComparator(){
    List<DinaRole> dinaRoleList = Arrays.asList(DinaRole.COLLECTION_MANAGER, DinaRole.DINA_ADMIN, DinaRole.STAFF, DinaRole.STUDENT);
    Collections.shuffle(dinaRoleList);

    dinaRoleList.sort(new PriorityComparator());
    assertEquals(DinaRole.DINA_ADMIN, dinaRoleList.get(0));
    assertEquals(DinaRole.COLLECTION_MANAGER, dinaRoleList.get(1));
    assertEquals(DinaRole.STAFF, dinaRoleList.get(2));
    assertEquals(DinaRole.STUDENT, dinaRoleList.get(3));

  }


  private static DinaAuthenticatedUser getDinaAuthenticatedUser(DinaRole dinaRole) {
    return DinaAuthenticatedUser.builder()
      .username("name")
      .rolesPerGroup(ImmutableMap.of("group1", ImmutableSet.of(dinaRole)))
      .build();
  }
}
