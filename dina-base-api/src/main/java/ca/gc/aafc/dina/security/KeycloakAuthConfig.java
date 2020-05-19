package ca.gc.aafc.dina.security;

import javax.inject.Inject;

import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.adapters.springboot.KeycloakSpringBootConfigResolver;
import org.keycloak.adapters.springsecurity.authentication.KeycloakAuthenticationProvider;
import org.keycloak.adapters.springsecurity.config.KeycloakWebSecurityConfigurerAdapter;
import org.keycloak.adapters.springsecurity.filter.KeycloakAuthenticationProcessingFilter;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
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
