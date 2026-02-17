package ca.gc.aafc.dina.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import jakarta.inject.Inject;

import ca.gc.aafc.dina.security.oauth2.DinaAuthenticationToken;
import ca.gc.aafc.dina.testsupport.PostgresTestContainerInitializer;
import org.apache.commons.collections.CollectionUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;

import ca.gc.aafc.dina.TestDinaBaseApp;

import org.springframework.test.context.ContextConfiguration;

@SpringBootTest(
  classes = TestDinaBaseApp.class,
  properties = "keycloak.enabled: true"
)
@ContextConfiguration(initializers = { PostgresTestContainerInitializer.class })
public class KeycloakCurrentUserIT {

  @Inject
  private DinaAuthenticatedUser currentUser;


  @Test
  public void getCurrentUser_WhenKeycloakGroupRolesClaims_GroupRolesParsed() {

    // Keycloak includes a forward slash to all group
    List<String> keycloakGroupClaim = Arrays.asList("/group 1/user", "/group 2/super-user");
    List<String> expectedGroups = Arrays.asList("group 1", "group 2");

    DinaAuthenticationToken mockToken = Mockito.mock(
      DinaAuthenticationToken.class,
      Answers.RETURNS_DEEP_STUBS);
    TestDinaBaseApp.mockToken(keycloakGroupClaim, mockToken);

    SecurityContextHolder.getContext().setAuthentication(mockToken);

    assertTrue(CollectionUtils.isEqualCollection(currentUser.getGroups(), expectedGroups));

    assertEquals(DinaRole.SUPER_USER, currentUser.getRolesPerGroup()
      .get("group 2").iterator().next());
  }
}
