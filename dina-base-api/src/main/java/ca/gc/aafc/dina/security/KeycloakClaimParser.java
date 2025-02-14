package ca.gc.aafc.dina.security;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import lombok.extern.log4j.Log4j2;

/**
 * Utility class to parse Keycloak claims.
 */
@Log4j2
public final class KeycloakClaimParser {

  private KeycloakClaimParser() {
  }

  /**
   * Parses a set of roles from Keycloak.
   * If the role is admin-based, it is added to adminRoles. Otherwise, the role is ignored.
   * @param roles
   * @return
   */
  public static Set<DinaRole> parseAdminRoles(Set<String> roles) {
    if (CollectionUtils.isEmpty(roles)) {
      return Collections.emptySet();
    }

    Set<DinaRole> adminRoles = new HashSet<>();
    for (String role : roles) {
      DinaRole.fromString(role).ifPresent(r -> {
        if (r.isAdminBased()) {
          adminRoles.add(r);
        }
      });
    }
    return adminRoles;
  }

  /**
   * Parses a list of Keycloak group claims and creates a Map of roles per group.
   * Expected format of the claim: /group/subgroup where the subgroup is matching 
   * the role. Unexpected formats and unknown group(s) will be ignored.
   * 
   * @param groupClaimList collection of group claims from Keycloak. Expected
   *                   structure /group/subgroup where subgroup also matches the
   *                   role
   * @return map representing the role(s) per group or an empty map
   */
  public static Map<String, Set<DinaRole>> parseGroupClaims(Collection<String> groupClaimList) {
    if (CollectionUtils.isEmpty(groupClaimList)) {
      return Collections.emptyMap();
    }

    Map<String, Set<DinaRole>> rolesPerGroup = new LinkedHashMap<>();

    for (String groupClaim : groupClaimList) {
      String[] claimParts = StringUtils.removeStart(groupClaim, "/").split("/");
      if (claimParts.length == 2) {
        rolesPerGroup.putIfAbsent(claimParts[0], new LinkedHashSet<>());

        log.debug(() -> claimParts[0] + ":" + DinaRole.fromString(claimParts[1]));

        // unknown roles will be ignored
        DinaRole.fromString(claimParts[1]).ifPresent(rolesPerGroup.get(claimParts[0])::add);
      } else if (claimParts.length == 1) {
        // mostly for backward compatibility
        log.info(() -> "Single element groupClaim, adding as group: " + claimParts[0]);
        rolesPerGroup.putIfAbsent(claimParts[0], new LinkedHashSet<>());
      } else {
        log.warn("Ignoring unknown groupClaim {}, claimParts: {}", groupClaim, claimParts);
      }
    }

    return rolesPerGroup;
  }
}
