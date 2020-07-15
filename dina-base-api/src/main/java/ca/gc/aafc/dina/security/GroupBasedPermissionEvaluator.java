package ca.gc.aafc.dina.security;

import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;

import ca.gc.aafc.dina.entity.DinaEntity;
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
    if (user == null || !(targetDomainObject instanceof DinaEntity)) {
      return false;
    }

    DinaEntity entity = (DinaEntity) targetDomainObject;
    Set<String> userGroups = user.getGroups();

    if (CollectionUtils.isEmpty(userGroups) || entity.getGroup() == null) {
      return false;
    }

    return userGroups.stream().anyMatch(entity.getGroup()::equalsIgnoreCase);
  }

  @Override
  public Object getThis() {
    return this;
  }

}
