package ca.gc.aafc.dina.security.oauth2;

import java.util.Collection;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import ca.gc.aafc.dina.security.DinaAuthenticatedUser;

public class DinaAuthenticationToken extends AbstractAuthenticationToken {
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
