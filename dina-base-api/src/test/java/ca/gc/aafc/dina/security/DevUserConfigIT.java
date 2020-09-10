package ca.gc.aafc.dina.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;

import com.google.common.collect.Sets;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import ca.gc.aafc.dina.TestDinaBaseApp;

@SpringBootTest(
  classes = TestDinaBaseApp.class,
  properties = {"dev-user.enabled: true", "keycloak.enabled: false"}
)
public class DevUserConfigIT {

  @Inject
  private DinaAuthenticatedUser currentUser;

  @Test
  public void getCurrentUser_whenKeycloakDisabled_devUserGiven() {
    assertEquals("dev", currentUser.getUsername());
    assertEquals("c628fc6f-c9ad-4bb6-a187-81eb7884bdd7", currentUser.getAgentIdentifer());
    assertEquals(Sets.newHashSet("dev-group"), currentUser.getGroups());
  }

}
