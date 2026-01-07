package ca.gc.aafc.dina.security.auth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ContextConfiguration;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.dto.ItemDto;
import ca.gc.aafc.dina.entity.Item;
import ca.gc.aafc.dina.jsonapi.JsonApiDocument;
import ca.gc.aafc.dina.jsonapi.JsonApiDocuments;
import ca.gc.aafc.dina.mapper.ItemMapper;
import ca.gc.aafc.dina.repository.DinaRepositoryV2;
import ca.gc.aafc.dina.service.AuditService;
import ca.gc.aafc.dina.service.DefaultDinaService;
import ca.gc.aafc.dina.testsupport.PostgresTestContainerInitializer;
import ca.gc.aafc.dina.testsupport.jsonapi.JsonAPITestHelper;
import ca.gc.aafc.dina.testsupport.security.WithMockKeycloakUser;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;
import java.util.UUID;
import javax.inject.Inject;
import javax.transaction.Transactional;

@Transactional
@SpringBootTest(classes = {TestDinaBaseApp.class, DinaAdminOnlyAuthTest.DinaAdminOnlyTestConfig.class},
  properties = "keycloak.enabled: true")
@ContextConfiguration(initializers = { PostgresTestContainerInitializer.class })
public class DinaAdminOnlyAuthTest {

  @Inject
  private DefaultDinaService<Item> defaultService;

  @Inject
  private DinaAdminCUDAuthorizationService authorizationService;

  @Inject
  private DinaRepositoryV2<ItemDto, Item> testRepo;

  private Item persistItem() {
    Item persisted = Item.builder().uuid(UUID.randomUUID()).group("group").build();
    defaultService.create(persisted);
    return persisted;
  }

  @Test
  @WithMockKeycloakUser(adminRole = {"DINA_ADMIN"})
  public void create_WhenAdmin_CreatesObject() {
    ItemDto dto = ItemDto.builder().uuid(UUID.randomUUID()).group("g").build();
    JsonApiDocument docToCreate = JsonApiDocuments.createJsonApiDocument(null, ItemDto.TYPE_NAME,
      JsonAPITestHelper.toAttributeMap(dto));

    ItemDto result = testRepo.create(docToCreate, null).getDto();
    assertNotNull(result.getUuid());
  }

  @Test
  @WithMockKeycloakUser(groupRole = {"CNC:SUPER_USER", "CNC:USER", "CNC:GUEST"})
  public void create_WhenNotAdmin_AccessDenied() {
    ItemDto dto = ItemDto.builder().uuid(UUID.randomUUID()).group("g").build();
    JsonApiDocument docToCreate = JsonApiDocuments.createJsonApiDocument(null, ItemDto.TYPE_NAME,
      JsonAPITestHelper.toAttributeMap(dto));

    assertThrows(AccessDeniedException.class, () -> testRepo.create(docToCreate, null));
  }

  @Test
  @WithMockKeycloakUser(adminRole = {"DINA_ADMIN"})
  void update_WhenAdmin_AccessAccepted() {

    Item persistedItem = persistItem();

    ItemDto dto = ItemDto.builder().uuid(persistedItem.getUuid()).group("g").build();
    JsonApiDocument docToUpdate = JsonApiDocuments.createJsonApiDocument(persistedItem.getUuid(), ItemDto.TYPE_NAME,
      JsonAPITestHelper.toAttributeMap(dto));

    assertDoesNotThrow(() -> testRepo.update(docToUpdate));
  }

  @Test
  @WithMockKeycloakUser(groupRole = {"CNC:SUPER_USER", "CNC:USER", "CNC:GUEST"})
  public void update_WhenNotAdmin_AccessDenied() {
    Item persistedItem = persistItem();
    ItemDto dto = ItemDto.builder().uuid(UUID.randomUUID()).group("g").build();
    JsonApiDocument docToUpdate = JsonApiDocuments.createJsonApiDocument(persistedItem.getUuid(), ItemDto.TYPE_NAME,
      JsonAPITestHelper.toAttributeMap(dto));

    assertThrows(AccessDeniedException.class, () -> testRepo.update(docToUpdate));
  }

  @Test
  @WithMockKeycloakUser(adminRole = {"DINA_ADMIN"})
  public void delete_WhenAdmin_AccessAccepted() {
    Item persistedItem = persistItem();
    assertDoesNotThrow(() -> testRepo.delete(persistedItem.getUuid()));
  }

  @Test
  @WithMockKeycloakUser(groupRole = {"CNC:SUPER_USER", "CNC:USER", "CNC:GUEST"})
  public void delete_WhenNotAdmin_AccessDenied() {
    Item persistedItem = persistItem();
    assertThrows(AccessDeniedException.class, () -> testRepo.delete(persistedItem.getUuid()));
  }

  @Test
  void getName() {
    assertEquals("DinaAdminCUDAuthorizationService", authorizationService.getName());
  }

  @TestConfiguration
  @EntityScan(basePackageClasses = DinaAdminOnlyAuthTest.class)
  static class DinaAdminOnlyTestConfig {

    // we can't use the repo from ItemTestConfig since we need DinaAdminCUDAuthorizationService
    @Bean
    @Primary
    public DinaRepositoryV2<ItemDto, Item> testRepo(
      Optional<AuditService> auditService,
      DinaAdminCUDAuthorizationService authorizationService,
      BuildProperties buildProperties,
      DefaultDinaService<Item> defaultService, ObjectMapper objMapper
    ) {
      return new DinaRepositoryV2<>(
        defaultService,
        authorizationService,
        auditService,
        ItemMapper.INSTANCE,
        ItemDto.class,
        Item.class,
        buildProperties, objMapper);
    }
  }
}
