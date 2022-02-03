package ca.gc.aafc.dina.security;

import lombok.extern.log4j.Log4j2;
import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.adapters.springboot.KeycloakSpringBootConfigResolver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Since KeycloakAuthConfig is extending KeycloakWebSecurityConfigurerAdapter it creates a circular dependency
 * with KeycloakConfigResolver.
 * This configuration class is simply to externalize the KeycloakConfigResolver bean creation.
 */
@Configuration
@ConditionalOnProperty(value = "keycloak.enabled", matchIfMissing = true)
@Log4j2
public class KeycloakConfigResolverConfig {

  @Bean
  public KeycloakConfigResolver keycloakConfigResolver() {
    log.debug("Creating KeycloakSpringBootConfigResolver bean");
    return new KeycloakSpringBootConfigResolver();
  }

}
