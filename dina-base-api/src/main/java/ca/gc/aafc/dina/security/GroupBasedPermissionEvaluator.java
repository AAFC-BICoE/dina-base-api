package ca.gc.aafc.dina.security;

import java.util.Set;

import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GroupBasedPermissionEvaluator extends SecurityExpressionRoot
    implements MethodSecurityExpressionOperations {

  private Object filterObject;
  private Object returnObject;

  public GroupBasedPermissionEvaluator(Authentication authentication) {
    super(authentication);
  }

  public boolean hasDinaPermission(DinaAuthenticatedUser user, Object targetDomainObject) {
    Set<String> userGroups = user.getGroups();
    return false;
  }

  @Override
  public Object getThis() {
    return this;
  }

}
