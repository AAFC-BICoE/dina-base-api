package ca.gc.aafc.dina.security.spring;

import ca.gc.aafc.dina.security.DinaRole;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@ConditionalOnProperty(value = "keycloak.enabled", matchIfMissing = true)
public class RoleAuthenticationProxy {

  @PreAuthorize("hasDinaRole(@currentUser, #roles)")
  public void hasDinaRole(Set<DinaRole> roles) {
  }

}
