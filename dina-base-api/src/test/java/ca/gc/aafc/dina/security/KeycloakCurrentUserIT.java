package ca.gc.aafc.dina.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;

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
    KeycloakAuthenticationToken mockToken = Mockito.mock(
      KeycloakAuthenticationToken.class,
      Answers.RETURNS_DEEP_STUBS
    );

    // Mock the needed fields on the keycloak token:
    Mockito.when(mockToken.getName()).thenReturn("test-user");
    Mockito.when(
      mockToken.getAccount()
        .getKeycloakSecurityContext()
        .getToken()
        .getOtherClaims()
        .get("agent-identifier"))
      .thenReturn("a2cef694-10f1-42ec-b403-e0f8ae9d2ae6");

    SecurityContextHolder.getContext().setAuthentication(mockToken);

    assertEquals("test-user", currentUser.getUsername());
    assertEquals("a2cef694-10f1-42ec-b403-e0f8ae9d2ae6", currentUser.getAgentIdentifer().toString());
  }

}
