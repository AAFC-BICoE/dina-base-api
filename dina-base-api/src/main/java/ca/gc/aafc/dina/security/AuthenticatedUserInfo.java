package ca.gc.aafc.dina.security;

import java.util.UUID;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class AuthenticatedUserInfo {
    
  private boolean inited = false;
  private UUID agentId;
  
  public boolean isInited() {
    return inited;
  }
  
  public UUID getAgentId() {
    return agentId;
  }
  
  public void setAgentId(UUID agentId) {
    if (isInited()) {
      log.debug("Can't set agent ID - already initialized");
      return;
    }
    
    this.agentId = agentId;
    inited = true;
  }
}
