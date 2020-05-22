package ca.gc.aafc.dina.security;

import java.util.UUID;

import javax.inject.Inject;

import org.keycloak.KeycloakPrincipal;
import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.adapters.springboot.KeycloakSpringBootConfigResolver;
import org.keycloak.adapters.springsecurity.authentication.KeycloakAuthenticationProvider;
import org.keycloak.adapters.springsecurity.config.KeycloakWebSecurityConfigurerAdapter;
import org.keycloak.adapters.springsecurity.filter.KeycloakAuthenticationProcessingFilter;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.mapping.SimpleAuthorityMapper;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;

import lombok.extern.log4j.Log4j2;

@Configuration
@ConditionalOnProperty(value = "keycloak.enabled", matchIfMissing = true)
@Log4j2
public class KeycloakAuthConfig extends KeycloakWebSecurityConfigurerAdapter {
  
  @Inject
  private AutowireCapableBeanFactory beanFactory;

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

  @Bean
  @Scope("session")
  public DinaAuthenticatedUser dinaAuthenticatedUser(@AuthenticationPrincipal KeycloakAuthenticationToken token) {
    Object principal = token.getPrincipal();
    if (principal instanceof KeycloakPrincipal<?>) {
      AccessToken accessToken = ((KeycloakPrincipal<?>) principal).getKeycloakSecurityContext().getToken();
      if (accessToken.getOtherClaims().containsKey(AGENT_IDENTIFIER_CLAIM_KEY)) {
        String agentId = (String) accessToken.getOtherClaims().get(AGENT_IDENTIFIER_CLAIM_KEY);
        //TODO handle failure
        return new DinaAuthenticatedUser(UUID.fromString(agentId));
      }
    }
    return null;
  }
  
  @Override
  protected void configure(HttpSecurity http) throws Exception {
    super.configure(http);
    
    // Need to disable CSRF for Postman and testing
    http.csrf().disable();
    
    http.authorizeRequests().anyRequest().authenticated();
    
    http.addFilterAfter(beanFactory.createBean(TestKeycloakFilter.class), KeycloakAuthenticationProcessingFilter.class);
    
    log.info("Configured HttpSecurity with filter");
  }
  
  @Override
  public void configure(WebSecurity web) throws Exception {
    // should be configurable
    // web.ignoring().antMatchers("/json-schema/**");
    log.debug("Configured WebSecurity");
  }

}
