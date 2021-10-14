package ca.gc.aafc.dina.security;

import ca.gc.aafc.dina.TestDinaBaseApp;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test making sure CORS configuration are correctly setup.
 */
@SpringBootTest(
  classes = TestDinaBaseApp.class,
  properties = {"dev-user.enabled: true", "keycloak.enabled: false", "cors.origins: git"}
)
public class CorsTesting {

  @Inject
  private CorsConfiguration corsConfiguration;

  @Test
  public void corsEnabled_whenOriginProvided_corsEnabled() {
    assertTrue(corsConfiguration.corsEnabled());
  }
}
