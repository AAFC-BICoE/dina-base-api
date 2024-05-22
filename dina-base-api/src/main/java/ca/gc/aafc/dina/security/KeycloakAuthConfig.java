package ca.gc.aafc.dina.security;

import java.util.Objects;
import lombok.extern.log4j.Log4j2;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.keycloak.adapters.springsecurity.authentication.KeycloakAuthenticationProvider;
import org.keycloak.adapters.springsecurity.config.KeycloakWebSecurityConfigurerAdapter;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.keycloak.representations.AccessToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer;
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
  private static final String IS_SERVICE_ACCOUNT_CLAIM_KEY = "is-service-account";
  private static final String GROUPS_CLAIM_KEY = "groups";

  @Value("${actuator.allowedIp:}")
  private String actuatorAllowedIp;

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
  @Override
  protected SessionAuthenticationStrategy sessionAuthenticationStrategy() {
    log.debug("Creating RegisterSessionAuthenticationStrategy bean");
    return new RegisterSessionAuthenticationStrategy(new SessionRegistryImpl());
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    super.configure(http);

    log.info("Configuring HttpSecurity");

    // Need to disable CSRF for Postman and testing
    ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry configurer =
        http.csrf().disable().authorizeRequests();

    // For Actuators endpoints
    if(StringUtils.isNotBlank(actuatorAllowedIp)) {
      configurer = configurer.requestMatchers(EndpointRequest.toAnyEndpoint()).hasIpAddress(actuatorAllowedIp);
      log.info("Actuator endpoints available for {}", actuatorAllowedIp);
    }

    configurer.anyRequest().authenticated();
  }

  @Override
  public void configure(WebSecurity web) throws Exception {
    // should be configurable
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

    AccessToken accessToken = token.getAccount()
      .getKeycloakSecurityContext()
      .getToken();

    Map<String, Object> otherClaims = accessToken.getOtherClaims();
    String agentId = (String) otherClaims.get(AGENT_IDENTIFIER_CLAIM_KEY);

    boolean isServiceAccount = BooleanUtils.toBoolean(
      Objects.toString(otherClaims.get(IS_SERVICE_ACCOUNT_CLAIM_KEY)));

    String internalID = accessToken.getSubject();

    Map<String, Set<DinaRole>> rolesPerGroup = null;
    if (otherClaims.get(GROUPS_CLAIM_KEY) instanceof Collection) {
      @SuppressWarnings("unchecked")
      Collection<String> groupClaim = (Collection<String>) otherClaims.get(GROUPS_CLAIM_KEY);
      rolesPerGroup = KeycloakClaimParser.parseGroupClaims(groupClaim);
    }

    return DinaAuthenticatedUser.builder()
      .agentIdentifier(agentId)
      .isServiceAccount(isServiceAccount)
      .internalIdentifier(internalID)
      .username(username)
      .rolesPerGroup(rolesPerGroup)
      .build();
  }

}
