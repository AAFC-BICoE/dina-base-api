package ca.gc.aafc.dina.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.junit.jupiter.api.Test;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.mockito.Answers;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;

import ca.gc.aafc.dina.TestConfiguration;

@SpringBootTest(
  classes = TestConfiguration.class,
  properties = "keycloak.enabled: true"
)
public class KeycloakCurrentUserIT {

  @Inject
  private DinaAuthenticatedUser currentUser;

  @Test
  public void getCurrentUser_whenLoggedIn_currentUserGiven() {
    List<String> expectedGroups = Arrays.asList("group 1", "group 2");

    KeycloakAuthenticationToken mockToken = Mockito.mock(
      KeycloakAuthenticationToken.class,
      Answers.RETURNS_DEEP_STUBS
    );

    mockToken(expectedGroups, mockToken);

    SecurityContextHolder.getContext().setAuthentication(mockToken);

    assertEquals("test-user", currentUser.getUsername());
    assertEquals("a2cef694-10f1-42ec-b403-e0f8ae9d2ae6", currentUser.getAgentIdentifer().toString());
    assertTrue(CollectionUtils.isEqualCollection(currentUser.getGroups(), expectedGroups));
  }

  @Test
  public void getCurrentUser_WhenKeycloakGroupRolesClaims_GroupRolesParsed() {

    // Keycloak includes a forward slash to all group
    List<String> keycloakGroupClaim = Arrays.asList("/group 1/staff", "/group 2/collection-manager");
    List<String> expectedGroups = Arrays.asList("group 1", "group 2");

    KeycloakAuthenticationToken mockToken = Mockito.mock(
      KeycloakAuthenticationToken.class,
      Answers.RETURNS_DEEP_STUBS);
    mockToken(keycloakGroupClaim, mockToken);

    SecurityContextHolder.getContext().setAuthentication(mockToken);

    assertTrue(CollectionUtils.isEqualCollection(currentUser.getGroups(), expectedGroups));

    assertEquals(DinaRole.COLLECTION_MANAGER, currentUser.getRolesPerGroup()
      .get("group 2").iterator().next());
  }

  /**
   * Mocks a given token to return a agent identifier and list of given groups.
   *
   * @param keycloakGroupClaim
   *                         - groups to return in claim
   * @param mockToken
   *                         - token to mock
   */
  private static void mockToken(List<String> keycloakGroupClaim, KeycloakAuthenticationToken mockToken) {
    // Mock the needed fields on the keycloak token:
    Mockito.when(mockToken.getName()).thenReturn("test-user");
    mockClaim(mockToken, "agent-identifier", "a2cef694-10f1-42ec-b403-e0f8ae9d2ae6");
    mockClaim(mockToken, "groups", keycloakGroupClaim);
  }

  /**
   * Mock a given tokens claims by returning a given value for the given claim
   * key.
   *
   * @param token
   *                - token holding claims
   * @param key
   *                - key of claim to mock
   * @param value
   *                - return value of the claim
   */
  private static void mockClaim(KeycloakAuthenticationToken token, String key, Object value) {
    Mockito.when(
        token.getAccount()
          .getKeycloakSecurityContext()
          .getToken()
          .getOtherClaims()
          .get(key))
      .thenReturn(value);
  }

}
