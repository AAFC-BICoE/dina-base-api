package ca.gc.aafc.dina.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.web.SecurityFilterChain;

import lombok.extern.log4j.Log4j2;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * A empty (permit all) security configuration file in order to prevent Spring Boot from 
 * automatically configuring its own security when Keycloak is disabled.
 * In production, Keycloak should always be enabled.
 *
 * Used for testing and local development.
 */
@Configuration
@ConditionalOnProperty(value = "keycloak.enabled", havingValue = "false")
@Log4j2
public class KeycloakDisabledAuthConfig {
  
  public KeycloakDisabledAuthConfig() {
    super();
    log.warn("⚠️ SECURITY DISABLED - Keycloak is OFF. All requests permitted!");
  }

  @Bean
  public SecurityFilterChain configure(HttpSecurity http) throws Exception {
    http
      .authorizeHttpRequests(authz -> authz
        .anyRequest().permitAll()
      )
      .httpBasic(withDefaults());
    return http.build();
  }

  @Bean
  public WebSecurityCustomizer webSecurityCustomizer() {
    return web -> web
      .ignoring()
      .anyRequest();
  }
}
