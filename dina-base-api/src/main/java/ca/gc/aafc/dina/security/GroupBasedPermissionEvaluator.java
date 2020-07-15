package ca.gc.aafc.dina.security;

import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;

import ca.gc.aafc.dina.entity.DinaEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * Custom spring security expression root. Can override or add custom spring
 * security expressions for use with spring method security.
 */
@Getter
@Setter
public class GroupBasedPermissionEvaluator extends SecurityExpressionRoot
    implements MethodSecurityExpressionOperations {

  /**
   * <p>
   * filterObject and returnObject are used to fullfill the requirements of
   * implementing the {@link MethodSecurityExpressionOperations}. Must be
   * implemented if you want to use filtering in method security expressions.
   * <p>
   * <p>
   * You can then use expressions such as
   * {@code @PostFilter("hasPermission(filterObject, 'READ')) }
   * <p>
   */
  private Object filterObject;

  /** see {@link GroupBasedPermissionEvaluator#filterObject} */
  private Object returnObject;

  public GroupBasedPermissionEvaluator(Authentication authentication) {
    super(authentication);
  }

  /**
   * Returns true if the given authenticated user is a member of the group the
   * given target object belongs to.
   * 
   * @param user               - Dina user being authenticated
   * @param targetDomainObject - Target resouce of the request
   * @return - true if the given authenticated user is a member of the group the
   *         given target object belongs to.
   */
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
