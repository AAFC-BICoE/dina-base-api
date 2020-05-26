package ca.gc.aafc.dina.security;

import java.io.IOException;
import java.util.UUID;

import javax.annotation.Resource;
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
  
  @Resource(name = "authenticatedUserInfo")
  private AuthenticatedUserInfo authenticatedUserInfo;
  
//  @Resource(name = "dinaAuthenticatedUser")
//  private DinaAuthenticatedUser dinaAuthenticatedUser;
  
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
      
      String agentId = null;
      
      if (accessToken.getOtherClaims().containsKey(AGENT_IDENTIFIER_CLAIM_KEY)) {
        agentId = (String) accessToken.getOtherClaims().get(AGENT_IDENTIFIER_CLAIM_KEY);
        log.info("Got agent id {}", agentId);
        request.setAttribute(AGENT_ID_REQUEST_ATTR, agentId);
      } else {
        log.error("No agent id");
      }
      
//      DinaAuthenticatedUser userInfo = dinaAuthenticatedUser;
//      if (userInfo == null) {
//        log.error("no user info");
//      } else {
//        log.info("got user info - agent id {}", userInfo.getAgentIdentifer());
//      }
      
      AuthenticatedUserInfo userInfo = authenticatedUserInfo;
      
      if (userInfo.isInited()) {
        final String infoAgentId = userInfo.getAgentId().toString();
        log.info("Authenticated user - agent id {}", infoAgentId);
      } else {
        log.info("Authentication not initialized - setting agent ID");
        userInfo.setAgentId(UUID.fromString(agentId));
      }
      
    }
    
    chain.doFilter(request, response);
  }

}
