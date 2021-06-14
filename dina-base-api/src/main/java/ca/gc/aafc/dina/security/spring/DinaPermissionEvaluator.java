package ca.gc.aafc.dina.security.spring;

import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.security.DinaAuthenticatedUser;
import ca.gc.aafc.dina.security.DinaRole;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;

import java.util.Map;
import java.util.Set;

/**
 * Custom spring security expression root. Can override or add custom spring security expressions for use with
 * spring method security.
 */
@Getter
@Setter
public class DinaPermissionEvaluator extends SecurityExpressionRoot
  implements MethodSecurityExpressionOperations {

  /**
   * <p>
   * filterObject and returnObject are used to fullfill the requirements of implementing the {@link
   * MethodSecurityExpressionOperations}. Must be implemented if you want to use filtering in method security
   * expressions.
   * <p>
   * <p>
   * You can then use expressions such as {@code @PostFilter("hasPermission(filterObject, 'READ')) }
   * <p>
   */
  private Object filterObject;

  /**
   * see {@link DinaPermissionEvaluator#filterObject}
   */
  private Object returnObject;

  public DinaPermissionEvaluator(Authentication authentication) {
    super(authentication);
  }

  /**
   * Returns true if the given authenticated user is a member of the group the given target object belongs
   * to.
   *
   * @param user               - Dina user being authenticated
   * @param targetDomainObject - Target resouce of the request
   * @return - true if the given authenticated user is a member of the group the given target object belongs
   * to.
   */
  public boolean hasGroupPermission(DinaAuthenticatedUser user, Object targetDomainObject) {
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

  /**
   * returns true if the given user has a given role in one of it's many groups.
   *
   * @param user user with roles
   * @param role role to check for
   * @return - true if the given user has a given role in one of it's many groups
   */
  public boolean hasDinaRole(DinaAuthenticatedUser user, String role) {
    if (user == null || StringUtils.isBlank(role)) {
      return false;
    }

    Map<String, Set<DinaRole>> rolesPerGroup = user.getRolesPerGroup();
    if (MapUtils.isEmpty(rolesPerGroup)) {
      return false;
    }

    return rolesPerGroup.values()
      .stream()
      .flatMap(Set::stream)
      .anyMatch(dinaRole -> dinaRole.name().equalsIgnoreCase(role.strip()));
  }

  /**
   * returns true if the given user has a given role in the given group.
   *
   * @param user  user with roles
   * @param role  role to check for
   * @param group group to check
   * @return true if the given user has a given role in the given group.
   */
  public boolean hasDinaRoleInGroup(DinaAuthenticatedUser user, String role, String group) {
    if (user == null || StringUtils.isBlank(role) || StringUtils.isBlank(group)) {
      return false;
    }

    String sanitizedGroup = group.strip();

    Map<String, Set<DinaRole>> rolesPerGroup = user.getRolesPerGroup();
    if (MapUtils.isEmpty(rolesPerGroup)
      || rolesPerGroup.keySet().stream().noneMatch(key -> key.equalsIgnoreCase(sanitizedGroup))) {
      return false;
    }

    return rolesPerGroup.entrySet()
      .stream()
      .filter(entry -> entry.getKey().equalsIgnoreCase(sanitizedGroup))
      .map(Map.Entry::getValue)
      .flatMap(Set::stream)
      .anyMatch(dinaRole -> dinaRole.name().equalsIgnoreCase(role.strip()));
  }

  @Override
  public Object getThis() {
    return this;
  }

}
