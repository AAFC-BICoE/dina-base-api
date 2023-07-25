package ca.gc.aafc.dina.security;

import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.MapUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.annotation.RequestScope;

import ca.gc.aafc.dina.config.DevSettings;

/**
 * Provides a dev user for local development testing without needing to sign in manually.
 * Keycloak must be disabled, or else you get an error about multiple DinaAuthenticatedUser beans.
 */
@Configuration
@ConditionalOnProperty(value = "dev-user.enabled", havingValue = "true")
public class DevUserConfig {

  private final DevSettings devSettings;

  public DevUserConfig(DevSettings devSettings) {
    this.devSettings = devSettings;
  }

  @Bean
  @RequestScope
  public DinaAuthenticatedUser currentUser() {

    DinaAuthenticatedUser.DinaAuthenticatedUserBuilder authenticatedUserBuilder =
      DinaAuthenticatedUser.builder()
        .agentIdentifier("c628fc6f-c9ad-4bb6-a187-81eb7884bdd7")
        .internalIdentifier("c628fc6f-c9ad-4bb6-a187-81eb7884bdd7")
        .username("dev");

    if(MapUtils.isNotEmpty(devSettings.getRolesPerGroup())) {
      authenticatedUserBuilder.rolesPerGroup(devSettings.getRolesPerGroup());
    } else{
      authenticatedUserBuilder.rolesPerGroup(Map.of("dev-group", Set.of(DinaRole.USER)));
    }

    return authenticatedUserBuilder.build();
  }

}
