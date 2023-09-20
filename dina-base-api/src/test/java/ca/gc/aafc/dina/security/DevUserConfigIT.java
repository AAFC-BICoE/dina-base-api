package ca.gc.aafc.dina.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import javax.inject.Inject;

import ca.gc.aafc.dina.testsupport.PostgresTestContainerInitializer;

import org.apache.commons.collections.CollectionUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import ca.gc.aafc.dina.TestDinaBaseApp;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest(
  classes = TestDinaBaseApp.class,
  properties = {"dev-user.enabled: true", "keycloak.enabled: false"}
)
@ContextConfiguration(initializers = { PostgresTestContainerInitializer.class })
public class DevUserConfigIT {

  @Inject
  private DinaAuthenticatedUser currentUser;

  // this test relies on the value from test/application.yml
  @Test
  public void getCurrentUser_whenKeycloakDisabled_devUserGiven() {
    assertEquals("dev", currentUser.getUsername());
    assertEquals("c628fc6f-c9ad-4bb6-a187-81eb7884bdd7", currentUser.getAgentIdentifier());
    assertTrue(CollectionUtils.isEqualCollection(Set.of("aafc", "bicoe"), currentUser.getGroups()), "groups not matching");
  }

}
