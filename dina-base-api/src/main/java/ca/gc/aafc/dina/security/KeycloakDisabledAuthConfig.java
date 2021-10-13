package ca.gc.aafc.dina.security;

import lombok.extern.log4j.Log4j2;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * A empty (permit all) security configuration file in order to prevent Spring Boot from 
 * automatically configuring its own security when Keycloak is disabled.
 * In production, Keycloak should always be enabled.
 */
@Configuration
@ConditionalOnProperty(value = "keycloak.enabled", havingValue = "false")
@Log4j2
public class KeycloakDisabledAuthConfig extends WebSecurityConfigurerAdapter {
  
  public KeycloakDisabledAuthConfig() {
    super();
    log.warn("Keycloak DISABLED. KeycloakDisabledAuthConfig created");
  }
  
  @Override
  protected void configure(HttpSecurity http) throws Exception {
    http.cors();

    http.csrf().disable();
    http.authorizeRequests().antMatchers("/**").permitAll();
  }
  
  @Override
  public void configure(WebSecurity web) throws Exception {
    web.ignoring().antMatchers("/**");
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    return CorsConfigurationFactory.buildCorsConfigurationSource();
  }

}
