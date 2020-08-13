package ca.gc.aafc.dina;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ca.gc.aafc.dina.security.DinaAuthenticatedUser;
import ca.gc.aafc.dina.security.DinaRole;

/** When you need an Authenticated user bean */
@Configuration
public class DinaUserConfig {

  public static final String AUTH_USER_NAME = "username";
  public static final Map<String, Set<DinaRole>> ROLES_PER_GROUP = ImmutableMap.of("group 1",
      ImmutableSet.of(DinaRole.COLLECTION_MANAGER));

  @Bean
  public DinaAuthenticatedUser user() {
    return DinaAuthenticatedUser.builder()
      .username(DinaUserConfig.AUTH_USER_NAME)
      .rolesPerGroup(ROLES_PER_GROUP)
      .build();
  }
}
