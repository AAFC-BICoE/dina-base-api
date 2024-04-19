package ca.gc.aafc.dina.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import ca.gc.aafc.dina.security.DinaRole;

/**
 * Settings only for devs in dev mode
 */
@Component
@ConfigurationProperties(prefix = "dev-user")
@Getter
@Setter
public class DevSettings {

  private Map<String, Set<String>> groupRole;

  public Map<String, Set<DinaRole>> getRolesPerGroup() {

    if(groupRole == null || groupRole.isEmpty()) {
      return Map.of();
    }

    Map<String, Set<DinaRole>> groupDinaRole = new HashMap<>(groupRole.size());

    for(var entry : groupRole.entrySet()) {
      groupDinaRole.put(entry.getKey(), entry.getValue().stream()
        .map(DinaRole::fromString)
        .map(Optional::orElseThrow).collect(
        Collectors.toSet()));
    }

    return groupDinaRole;
  }

}
