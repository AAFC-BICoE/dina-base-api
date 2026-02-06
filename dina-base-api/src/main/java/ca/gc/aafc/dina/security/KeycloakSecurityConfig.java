package ca.gc.aafc.dina.security;

import org.apache.commons.lang3.BooleanUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.context.annotation.RequestScope;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

// WORK-IN-PROGRESS ----
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(
  securedEnabled = true,
  jsr250Enabled = true
)
public class KeycloakSecurityConfig {

  private static final String AGENT_IDENTIFIER_CLAIM_KEY = "agent-identifier";
  private static final String IS_SERVICE_ACCOUNT_CLAIM_KEY = "is-service-account";
  private static final String GROUPS_CLAIM_KEY = "groups";


  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http
      .authorizeHttpRequests(authz -> authz
        .anyRequest().authenticated()
      )
      .sessionManagement(session -> session
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
      )
      .oauth2ResourceServer(oauth2 -> oauth2
        .jwt(jwt -> jwt
          .jwtAuthenticationConverter(jwtAuthenticationConverter())
        )
      )
      .cors(Customizer.withDefaults())
      .csrf(AbstractHttpConfigurer::disable)
      .formLogin(AbstractHttpConfigurer::disable)
      .httpBasic(AbstractHttpConfigurer::disable)
      .build();
  }

  @Bean
  public Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {

    return jwt -> {
      // Extract everything once
      DinaAuthenticatedUser user = DinaAuthenticatedUser.builder()
        .username(jwt.getClaimAsString("preferred_username"))
        .internalIdentifier(jwt.getSubject())
        .agentIdentifier((String) jwt.getClaims().get(AGENT_IDENTIFIER_CLAIM_KEY))
        .isServiceAccount(extractIsServiceAccount(jwt))
        .rolesPerGroup(extractRolesPerGroup(jwt))
        .adminRoles(extractAdminRoles(jwt))
        .build();

      // Store in a custom authentication token
      DinaAuthenticationToken token = new DinaAuthenticationToken(
        jwt,
        user,
        convertToGrantedAuthorities(user)
      );

      return token;
    };
  }

  @Bean
  @RequestScope
  public DinaAuthenticatedUser currentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication instanceof DinaAuthenticationToken) {
      return ((DinaAuthenticationToken) authentication).getUser();
    }

    return null;
  }

  private Collection<? extends GrantedAuthority> convertToGrantedAuthorities(
    DinaAuthenticatedUser user) {

    Set<GrantedAuthority> authorities = new HashSet<>();

    // Convert admin roles
    if (user.getAdminRoles() != null) {
      user.getAdminRoles().stream()
        .map(dinaRole -> new SimpleGrantedAuthority("ROLE_" + dinaRole.name()))
        .forEach(authorities::add);
    }

    // Convert group-based roles (if applicable)
    if (user.getRolesPerGroup() != null) {
      user.getRolesPerGroup().values().stream()
        .flatMap(Set::stream)
        .map(dinaRole -> new SimpleGrantedAuthority("ROLE_" + dinaRole.name()))
        .forEach(authorities::add);
    }

    return authorities;
  }

  // Custom authentication token
  static class DinaAuthenticationToken extends AbstractAuthenticationToken {
    private final Jwt jwt;
    private final DinaAuthenticatedUser user;

    public DinaAuthenticationToken(Jwt jwt, DinaAuthenticatedUser user,
                                   Collection<? extends GrantedAuthority> authorities) {
      super(authorities);
      this.jwt = jwt;
      this.user = user;
      setAuthenticated(true);
    }

    public DinaAuthenticatedUser getUser() {
      return user;
    }

    @Override
    public Object getCredentials() {
      return jwt.getTokenValue();
    }

    @Override
    public Object getPrincipal() {
      return user.getUsername();
    }
  }

  private boolean extractIsServiceAccount(Jwt jwt) {
    Object value = jwt.getClaims().get(IS_SERVICE_ACCOUNT_CLAIM_KEY);
    return BooleanUtils.toBoolean(Objects.toString(value));
  }

  private Map<String, Set<DinaRole>> extractRolesPerGroup(Jwt jwt) {
    Object groupClaim = jwt.getClaims().get(GROUPS_CLAIM_KEY);

    if (groupClaim instanceof Collection) {
      @SuppressWarnings("unchecked")
      Collection<String> groups = (Collection<String>) groupClaim;
      return KeycloakClaimParser.parseGroupClaims(groups);
    }
    return null;
  }

  private Set<DinaRole> extractAdminRoles(Jwt jwt) {
    Map<String, Object> realmAccess =
      (Map<String, Object>) jwt.getClaims().get("realm_access");

    if (realmAccess != null && realmAccess.get("roles") instanceof Collection) {
      @SuppressWarnings("unchecked")
      Collection<String> roles = (Collection<String>) realmAccess.get("roles");
      return KeycloakClaimParser.parseAdminRoles(new HashSet<>(roles));
    }

    return Collections.emptySet();
  }
}
