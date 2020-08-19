package ca.gc.aafc.dina.security;

import ca.gc.aafc.dina.security.spring.DinaPermissionEvaluator;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.mockito.Answers;
import org.mockito.Mockito;

import java.util.Map;
import java.util.Set;

public class DinaPermissionEvaluatorTest {

  private static final ImmutableSet<DinaRole> ROLE_TO_COMPARE = ImmutableSet.of(DinaRole.COLLECTION_MANAGER);
  private DinaPermissionEvaluator evaluator;

  @BeforeEach
  void setUp() {
    KeycloakAuthenticationToken mockToken = Mockito.mock(
      KeycloakAuthenticationToken.class,
      Answers.RETURNS_DEEP_STUBS);
    evaluator = new DinaPermissionEvaluator(mockToken);
  }

  @Test
  public void hadDinaRole_hasRole_ReturnsTrue() {
    DinaAuthenticatedUser user = createWithRoles(ROLE_TO_COMPARE);
    Assertions.assertTrue(evaluator.hasDinaRole(user, ROLE_TO_COMPARE));
  }

  @Test
  public void hadDinaRole_DoesNotHaveRole_ReturnsFalse() {
    DinaAuthenticatedUser user = createWithRoles(ImmutableSet.of(DinaRole.STAFF));
    Assertions.assertFalse(evaluator.hasDinaRole(user, ROLE_TO_COMPARE));
  }

  @Test
  public void hadDinaRole_WhenUserNull_ReturnsFalse() {
    Assertions.assertFalse(evaluator.hasDinaRole(null, ROLE_TO_COMPARE));
  }

  @Test
  public void hadDinaRole_WhenRolesNull_ReturnsFalse() {
    DinaAuthenticatedUser user = createWithRoles(ImmutableSet.of(DinaRole.COLLECTION_MANAGER));
    Assertions.assertFalse(evaluator.hasDinaRole(user, null));
  }

  private static DinaAuthenticatedUser createWithRoles(ImmutableSet<DinaRole> dinaRoles) {
    Map<String, Set<DinaRole>> roles = ImmutableMap.of("group 1", dinaRoles);
    return DinaAuthenticatedUser.builder().rolesPerGroup(roles).build();
  }

}
