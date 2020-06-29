package ca.gc.aafc.dina.security;

import com.google.common.collect.Sets;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.annotation.RequestScope;

/**
 * Provides a dev user for local development testing without needing to sign in manually.
 * Keycloak must be disabled, or else you get an error about multiple DinaAuthenticatedUser beans.
 */
@Configuration
@ConditionalOnProperty(value = "dev-user.enabled", havingValue = "true")
public class DevUserConfig {

  @Bean
  @RequestScope
  public DinaAuthenticatedUser currentUser() {
    return DinaAuthenticatedUser.builder()
      .agentIdentifer("c628fc6f-c9ad-4bb6-a187-81eb7884bdd7")
      .username("dev")
      .groups(Sets.newHashSet("dev-group"))
      .build();
  }

}
