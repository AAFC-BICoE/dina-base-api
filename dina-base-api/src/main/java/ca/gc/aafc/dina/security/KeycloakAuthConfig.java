package ca.gc.aafc.dina.security;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
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
  private static final String GROUPS_CLAIM_KEY = "groups";

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

    String agentId = (String) otherClaims.get(AGENT_IDENTIFIER_CLAIM_KEY);

    Set<String> groups = new LinkedHashSet<>();
    if (otherClaims.get(GROUPS_CLAIM_KEY) instanceof Collection) {
      @SuppressWarnings("unchecked")
      Collection<String> groupClaim = (Collection<String>) otherClaims.get(GROUPS_CLAIM_KEY);
      groups.addAll(removePrefix("/", groupClaim));
    }

    return DinaAuthenticatedUser.builder()
      .agentIdentifer(agentId)
      .username(username)
      .groups(groups)
      .build();
  }

  /**
   * Returns a set of strings matching a given collection with a given prefix
   * removed from the colletion elements.
   * 
   * @param prefix
   *                     - prefix to remove
   * @param collection
   *                     - collection to iterate
   * @return
   */
  private static Set<String> removePrefix(String prefix, Collection<String> collection) {
    if (CollectionUtils.isEmpty(collection)) {
      return new HashSet<>();
    } else {
      return collection.stream().map(grp -> StringUtils.removeStart(grp, prefix))
          .collect(Collectors.toSet());
    }
  }

}
