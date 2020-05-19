package ca.gc.aafc.dina.security;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.keycloak.representations.AccessToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.GenericFilterBean;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class TestKeycloakFilter extends GenericFilterBean {
  
  // TODO possibly move these constants somewhere else
  public static final String AGENT_ID_REQUEST_ATTR = "ca.gc.aafc.dina.agent.identifier";
  
  private static final String AGENT_IDENTIFIER_CLAIM_KEY = "agent-identifier";
  
  private void logme(final Object o, final String n) {
    if (!log.isDebugEnabled()) {
      return;
    }
    
    final StringBuilder sb = new StringBuilder();
    sb.append(n);
    sb.append(": ");
    sb.append(o.getClass().getCanonicalName());
    sb.append(" - ");
    sb.append(o.toString());
    
    log.debug(sb.toString());
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    
    logme(authentication, "authentication");
    
    if (authentication instanceof KeycloakAuthenticationToken) {
      log.debug("Got a KeycloakAuthenticationToken");
      
      final KeycloakAuthenticationToken kcAuth = (KeycloakAuthenticationToken) authentication;
      final KeycloakPrincipal<?> principal = (KeycloakPrincipal<?>) kcAuth.getPrincipal();
      final KeycloakSecurityContext secContext = principal.getKeycloakSecurityContext();
      final AccessToken accessToken = secContext.getToken();
      
      logme(kcAuth, "kcAuth");
      logme(principal, "principal");
      logme(secContext, "secContext");
      logme(accessToken, "accessToken");
      
      if (accessToken.getOtherClaims().containsKey(AGENT_IDENTIFIER_CLAIM_KEY)) {
        final String agentId = (String) accessToken.getOtherClaims().get(AGENT_IDENTIFIER_CLAIM_KEY);
        log.info("Got agent id {}", agentId);
        request.setAttribute(AGENT_ID_REQUEST_ATTR, agentId);
      } else {
        log.error("No agent id");
      }
      
    }
    
    chain.doFilter(request, response);
  }

}
