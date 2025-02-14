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
import java.util.Optional;
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
   * @param targetDomainObject - Target resource of the request
   * @return - true if the given authenticated user is a member of the group the given target object belongs
   * to.
   */
  public boolean hasGroupPermission(DinaAuthenticatedUser user, Object targetDomainObject) {
    if (user == null || !(targetDomainObject instanceof DinaEntity entity)) {
      return false;
    }

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
   * returns true if the given user has a given admin role
   *
   * @param user user with roles
   * @param role admin role to check for
   * @return - true if the given user has a given role in one of it's many groups
   */
  public boolean hasAdminRole(DinaAuthenticatedUser user, String role) {
    if (user == null || StringUtils.isBlank(role)) {
      return false;
    }

    return user.getAdminRoles()
      .stream()
      .anyMatch(dinaRole -> dinaRole.name().equalsIgnoreCase(role.strip()));
  }

  /**
   * Returns true if the given authenticated user is a member of the group the given target object belongs to
   * and also has the given role for that group.
   *
   * @param user               user with roles
   * @param role               role to check for
   * @param targetDomainObject Target resource of the request
   * @return true if the given user has a given role in the given group.
   */
  public boolean hasGroupAndRolePermissions(
    DinaAuthenticatedUser user,
    String role,
    Object targetDomainObject
  ) {
    if (user == null || StringUtils.isBlank(role) || !(targetDomainObject instanceof DinaEntity)) {
      return false;
    }

    Optional<Set<DinaRole>> roles = user.getRolesForGroup(((DinaEntity) targetDomainObject).getGroup());

    if (roles.isEmpty()) {
      return false;
    }
    return roles.get().stream().anyMatch(dinaRole -> dinaRole.name().equalsIgnoreCase(role.strip()));
  }

  /**
   * Returns true if the given authenticated user is a member of the group the given target object belongs to
   * and also has the given minimum role for that group.
   *
   * @param user               user with roles
   * @param minimumRole        minimum role to check the user has
   * @param targetDomainObject Target resource of the request
   * @return true if the given user has the given minimum role for that group.
   */
  public boolean hasMinimumGroupAndRolePermissions(
    DinaAuthenticatedUser user,
    String minimumRole,
    Object targetDomainObject
  ) {
    if (user == null || StringUtils.isBlank(minimumRole) || !(targetDomainObject instanceof DinaEntity)) {
      return false;
    }

    Optional<DinaRole> minimumDinaRole = DinaRole.fromString(minimumRole);
    Optional<Set<DinaRole>> roles = user.getRolesForGroup(((DinaEntity) targetDomainObject).getGroup());

    if (roles.isEmpty() || minimumDinaRole.isEmpty()) {
      return false;
    }

    return roles.get().stream().anyMatch(dinaRole -> dinaRole.isHigherOrEqualThan(minimumDinaRole.get()));
  }

  /**
   * Returns true if the given authenticated user belongs to any group that is equal or higher than the
   * given minimum group.
   * 
   * @param user                User with roles to check against.
   * @param minimumRole         The minimum role to check the user has.
   * @return true if the given user has any group that is equal or higher than the given minimum role.
   */
  public boolean hasMinimumDinaRole(
    DinaAuthenticatedUser user,
    String minimumRole
  ) {
    // Ensure all provided arguments are valid.
    if (user == null || StringUtils.isBlank(minimumRole)) {
      return false;
    }

    // Using the string of the dina role, convert it into a DinaRole object (if possible).
    Optional<DinaRole> minimumDinaRole = DinaRole.fromString(minimumRole);

    // Retrieve all of the roles for the user.
    Map<String, Set<DinaRole>> rolesPerGroup = user.getRolesPerGroup();
    if (MapUtils.isEmpty(rolesPerGroup) || minimumDinaRole.isEmpty()) {
      return false;
    }

    // Go through all of the roles the user has, and if any of them are higher or equal to the given
    // minimum role it will return true.
    return rolesPerGroup.values()
      .stream()
      .flatMap(Set::stream)
      .anyMatch(dinaRole -> dinaRole.isHigherOrEqualThan(minimumDinaRole.get()));
  }

  /**
   * Returns true if the given authenticated user is interpreted as the owner of an object.
   * owner is defined by the value of createdBy in {@link DinaEntity}.
   * @param user authenticated user
   * @param targetDomainObject object to check ownership
   * @return true if the targetDomainObject createdBy equals the authenticated username.
   */
  public boolean hasObjectOwnership(DinaAuthenticatedUser user, Object targetDomainObject) {
    if (user == null || !(targetDomainObject instanceof DinaEntity)) {
      return false;
    }
    return StringUtils.equals(user.getUsername(), ((DinaEntity) targetDomainObject).getCreatedBy());
  }

  /**
   * Check if the target object is publicly releasable.
   * @param targetDomainObject object to check
   * @return true if isPubliclyReleasable returns TRUE, false otherwise (including null)
   */
  public boolean isObjectPubliclyReleasable(Object targetDomainObject) {
    if (!(targetDomainObject instanceof DinaEntity)) {
      return false;
    }
    return ((DinaEntity) targetDomainObject).isPubliclyReleasable().orElse(false);
  }

  @Override
  public Object getThis() {
    return this;
  }

}
