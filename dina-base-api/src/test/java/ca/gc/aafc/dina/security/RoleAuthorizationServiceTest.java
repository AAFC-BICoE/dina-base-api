package ca.gc.aafc.dina.security;

import ca.gc.aafc.dina.service.RoleAuthorizationService;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.crnk.core.exception.ForbiddenException;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertThrows;

public class RoleAuthorizationServiceTest {

  private static final Map<String, Set<DinaRole>> VALID_ROLES = ImmutableMap.of(
    "group 1",
    ImmutableSet.of(DinaRole.COLLECTION_MANAGER));
  private static final Map<String, Set<DinaRole>> INVALID_ROLES = ImmutableMap.of(
    "group 1",
    ImmutableSet.of(DinaRole.STAFF));

  @Test
  void create_Authorized_AllowsOperation() {
    RoleAuthorizationService service = new RoleAuthorizationService(
      DinaRole.COLLECTION_MANAGER,
      getUser(VALID_ROLES));

    service.authorizeCreate("obj");
  }

  @Test
  void create_Unauthorized_ThrowsForbidden() {
    RoleAuthorizationService service = new RoleAuthorizationService(
      DinaRole.COLLECTION_MANAGER,
      getUser(INVALID_ROLES));

    assertThrows(ForbiddenException.class, () -> service.authorizeCreate("obj"));

  }

  @Test
  void update_Authorized_AllowsOperation() {
    RoleAuthorizationService service = new RoleAuthorizationService(
      DinaRole.COLLECTION_MANAGER,
      getUser(VALID_ROLES));

    service.authorizeUpdate("obj");
  }

  @Test
  void update_Unauthorized_ThrowsForbidden() {
    RoleAuthorizationService service = new RoleAuthorizationService(
      DinaRole.COLLECTION_MANAGER,
      getUser(INVALID_ROLES));

    assertThrows(ForbiddenException.class, () -> service.authorizeUpdate("obj"));
  }

  @Test
  void delete_Authorized_AllowsOperation() {
    RoleAuthorizationService service = new RoleAuthorizationService(
      DinaRole.COLLECTION_MANAGER,
      getUser(VALID_ROLES));

    service.authorizeDelete("obj");
  }

  @Test
  void delete_Unauthorized_ThrowsForbidden() {
    RoleAuthorizationService service = new RoleAuthorizationService(
      DinaRole.COLLECTION_MANAGER,
      getUser(INVALID_ROLES));

    assertThrows(ForbiddenException.class, () -> service.authorizeDelete("obj"));
  }

  private static DinaAuthenticatedUser getUser(Map<String, Set<DinaRole>> rolesPerGroup) {
    return DinaAuthenticatedUser.builder().rolesPerGroup(rolesPerGroup).username("name").build();
  }
}
