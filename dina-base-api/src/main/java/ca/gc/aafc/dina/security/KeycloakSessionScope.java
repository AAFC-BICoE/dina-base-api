package ca.gc.aafc.dina.security;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.keycloak.KeycloakPrincipal;
import org.keycloak.representations.AccessToken;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class KeycloakSessionScope implements Scope {
  
  private final Map<String, Object> scopedObjects = Collections.synchronizedMap(new HashMap<String, Object>());
  private final Map<String, Runnable> destructionCallbacks = Collections.synchronizedMap(new HashMap<String, Runnable>());

  @Override
  public Object get(String name, ObjectFactory<?> objectFactory) {
    if (!scopedObjects.containsKey(name)) {
      //TODO
      scopedObjects.put(name, objectFactory.getObject());
    }
    
    return scopedObjects.get(name);
  }

  @Override
  public Object remove(String name) {
    destructionCallbacks.remove(name);
    return scopedObjects.remove(name);
  }

  @Override
  public void registerDestructionCallback(String name, Runnable callback) {
    destructionCallbacks.put(name, callback);
  }

  @Override
  public Object resolveContextualObject(String key) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getConversationId() {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return getSessionStateFromAuthentication(authentication);
  }

  private static String getSessionStateFromAuthentication(final Authentication auth) {
    if (auth == null) {
      log.warn("No authentication");
      return null;
    }
    
    final Object principal = auth.getPrincipal();
    if (principal instanceof KeycloakPrincipal<?>) {
      final AccessToken accessToken = ((KeycloakPrincipal<?>) principal).getKeycloakSecurityContext().getToken();
      return accessToken.getSessionState();
    }
    
    log.warn("Authentication has invalid principal: {} {}", principal.getClass().getName(), principal.toString());
    return null;
  }

}
