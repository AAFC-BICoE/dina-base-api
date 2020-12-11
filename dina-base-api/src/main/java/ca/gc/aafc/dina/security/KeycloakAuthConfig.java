package ca.gc.aafc.dina.security;

import lombok.extern.log4j.Log4j2;
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

import javax.inject.Inject;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

@Configuration
@ConditionalOnProperty(value = "keycloak.enabled", matchIfMissing = true)
@Log4j2
public class KeycloakAuthConfig extends KeycloakWebSecurityConfigurerAdapter {

  private static final String AGENT_IDENTIFIER_CLAIM_KEY = "agent-identifier";
  private static final String GROUPS_CLAIM_KEY = "groups";
  private static final String INTERNAL_IDENTIFIER_CLAIM_KEY = "internal-identifier";

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
    String internalID = (String) otherClaims.getOrDefault(INTERNAL_IDENTIFIER_CLAIM_KEY, "");

    Map<String, Set<DinaRole>> rolesPerGroup = null;
    if (otherClaims.get(GROUPS_CLAIM_KEY) instanceof Collection) {
      @SuppressWarnings("unchecked")
      Collection<String> groupClaim = (Collection<String>) otherClaims.get(GROUPS_CLAIM_KEY);
      rolesPerGroup = KeycloakClaimParser.parseGroupClaims(groupClaim);
    }

    return DinaAuthenticatedUser.builder()
      .agentIdentifer(agentId)
      .internalIdentifer(internalID)
      .username(username)
      .rolesPerGroup(rolesPerGroup)
      .build();
  }

}
