package ca.gc.aafc.dina.security;

import org.apache.commons.lang3.BooleanUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.context.annotation.RequestScope;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.extern.log4j.Log4j2;

import ca.gc.aafc.dina.security.oauth2.DinaAuthenticationToken;

import static ca.gc.aafc.dina.security.KeycloakClaimParser.AGENT_IDENTIFIER_CLAIM_KEY;
import static ca.gc.aafc.dina.security.KeycloakClaimParser.GROUPS_CLAIM_KEY;
import static ca.gc.aafc.dina.security.KeycloakClaimParser.IS_SERVICE_ACCOUNT_CLAIM_KEY;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(
  securedEnabled = true,
  jsr250Enabled = true
)
@ConditionalOnProperty(value = "keycloak.enabled", matchIfMissing = true)
@Log4j2
public class KeycloakSecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    log.info("Creating SecurityFilterChain bean");
    SecurityFilterChain chain = http
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
    log.info("SecurityFilterChain bean created successfully");
    return chain;
  }

  @Bean
  public Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
    return new Converter<>() {
      @Override
      public AbstractAuthenticationToken convert(Jwt jwt) {

        log.debug("Converting JWT to AbstractAuthenticationToken for user: {}",
          jwt.getClaimAsString("preferred_username"));

        DinaAuthenticatedUser user = jwtToDinaAuthenticatedUser(jwt);
        // Store in a custom authentication token
        return new DinaAuthenticationToken(
          jwt,
          user,
          convertToGrantedAuthorities(user)
        );
      }
    };
  }

  @Bean
  @RequestScope
  public DinaAuthenticatedUser currentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication instanceof DinaAuthenticationToken) {
      return ((DinaAuthenticationToken) authentication).getUser();
    } else if ( authentication instanceof JwtAuthenticationToken jat) {
      return jwtToDinaAuthenticatedUser(jat.getToken());
    }
    log.warn("Could not resolve DinaAuthenticatedUser from authentication: {}",
      authentication != null ? authentication.getClass() : "null");
    return null;
  }

  private static DinaAuthenticatedUser jwtToDinaAuthenticatedUser(Jwt jwt) {
    return DinaAuthenticatedUser.builder()
      .username(jwt.getClaimAsString("preferred_username"))
      .internalIdentifier(jwt.getSubject())
      .agentIdentifier((String) jwt.getClaims().get(AGENT_IDENTIFIER_CLAIM_KEY))
      .isServiceAccount(extractIsServiceAccount(jwt))
      .rolesPerGroup(extractRolesPerGroup(jwt))
      .adminRoles(extractAdminRoles(jwt))
      .build();
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

  private static boolean extractIsServiceAccount(Jwt jwt) {
    Object value = jwt.getClaims().get(IS_SERVICE_ACCOUNT_CLAIM_KEY);
    return BooleanUtils.toBoolean(Objects.toString(value));
  }

  private static Map<String, Set<DinaRole>> extractRolesPerGroup(Jwt jwt) {
    Object groupClaim = jwt.getClaims().get(GROUPS_CLAIM_KEY);

    if (groupClaim instanceof Collection) {
      @SuppressWarnings("unchecked")
      Collection<String> groups = (Collection<String>) groupClaim;
      return KeycloakClaimParser.parseGroupClaims(groups);
    }
    return null;
  }

  private static Set<DinaRole> extractAdminRoles(Jwt jwt) {
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
