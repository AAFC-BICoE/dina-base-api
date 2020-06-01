package ca.gc.aafc.dina.security;

import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.adapters.springboot.KeycloakSpringBootConfigResolver;
import org.keycloak.adapters.springsecurity.authentication.KeycloakAuthenticationProvider;
import org.keycloak.adapters.springsecurity.config.KeycloakWebSecurityConfigurerAdapter;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.core.authority.mapping.SimpleAuthorityMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.web.context.annotation.RequestScope;

import lombok.extern.log4j.Log4j2;

@Configuration
@ConditionalOnProperty(value = "keycloak.enabled", matchIfMissing = true)
@Log4j2
public class KeycloakAuthConfig extends KeycloakWebSecurityConfigurerAdapter {

  private static final String AGENT_IDENTIFIER_CLAIM_KEY = "agent-identifier";

  public KeycloakAuthConfig() {
    super();
    log.info("KeycloakAuthConfig created");
  }
  
  @Inject
  public void configureGlobal(AuthenticationManagerBuilder auth) {
    KeycloakAuthenticationProvider keycloakAuthProvider = keycloakAuthenticationProvider();
    keycloakAuthProvider.setGrantedAuthoritiesMapper(new SimpleAuthorityMapper());
    auth.authenticationProvider(keycloakAuthProvider);
  }
  
  @Bean
  public KeycloakConfigResolver keycloakConfigResolver() {
    log.debug("Creating KeycloakSpringBootConfigResolver bean");
    return new KeycloakSpringBootConfigResolver();
  }
  
  @Bean
  @Override
  protected SessionAuthenticationStrategy sessionAuthenticationStrategy() {
    log.debug("Creating RegisterSessionAuthenticationStrategy bean");
    return new RegisterSessionAuthenticationStrategy(new SessionRegistryImpl());
  }
  
  @Override
  protected void configure(HttpSecurity http) throws Exception {
    super.configure(http);
    
    // Need to disable CSRF for Postman and testing
    http.csrf().disable();
    
    http.authorizeRequests().anyRequest().authenticated();
    log.debug("Configured HttpSecurity");
  }
  
  @Override
  public void configure(WebSecurity web) throws Exception {
    // should be configurable
    // web.ignoring().antMatchers("/json-schema/**");
    log.debug("Configured WebSecurity");
  }

  @Bean
  @RequestScope
  public DinaAuthenticatedUser currentUser() {
    KeycloakAuthenticationToken token = (KeycloakAuthenticationToken) SecurityContextHolder.getContext()
        .getAuthentication();

    if (token == null) {
      return null;
    }

    String username = token.getName();

    Map<String, Object> otherClaims = token.getAccount()
      .getKeycloakSecurityContext()
      .getToken()
      .getOtherClaims();

    UUID agentId = UUID.fromString((String) otherClaims.get(AGENT_IDENTIFIER_CLAIM_KEY));

    return DinaAuthenticatedUser.builder()
      .agentIdentifer(agentId)
      .username(username)
      .build();
  }

}
