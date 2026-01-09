package ca.gc.aafc.dina.security.auth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ContextConfiguration;

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

import org.springframework.boot.test.context.TestConfiguration;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.Optional;
import java.util.UUID;
import javax.inject.Inject;
import javax.transaction.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

@Transactional
@SpringBootTest(classes = {TestDinaBaseApp.class, ObjectOwnerAuthTest.ObjectOwnerAuthTestConfig.class},
  properties = "keycloak.enabled: true")
@ContextConfiguration(initializers = { PostgresTestContainerInitializer.class })
public class ObjectOwnerAuthTest {

  private static final String GROUP = "group";
  private static final String USER = "myuser";

  @Inject
  private DefaultDinaService<Item> defaultService;

  @Inject
  private DinaRepositoryV2<ItemDto, Item> testRepo;

  @Test
  @WithMockKeycloakUser(groupRole = {GROUP + ":SUPER_USER"}, username = USER)
  public void create_WhenNotCreatedBy_AccessDenied() {
    ItemDto dto = ItemDto.builder().uuid(UUID.randomUUID()).createdBy("xyz").group(GROUP).build();

    JsonApiDocument docToCreate = JsonApiDocuments.createJsonApiDocument(null, ItemDto.TYPE_NAME,
      JsonAPITestHelper.toAttributeMap(dto));

    assertThrows(AccessDeniedException.class, () -> testRepo.create(docToCreate, null));
  }

  @Test
  @WithMockKeycloakUser(groupRole = {GROUP + ":SUPER_USER"}, username = USER)
  void update_WhenCreatedBy_AccessAccepted() {
    Item myItem = Item.builder().uuid(UUID.randomUUID()).createdBy(USER).group(GROUP).build();
    defaultService.create(myItem);

    ItemDto dto = ItemDto.builder().uuid(myItem.getUuid()).createdBy(USER).group(GROUP).build();
    JsonApiDocument docToUpdate = JsonApiDocuments.createJsonApiDocument(myItem.getUuid(), ItemDto.TYPE_NAME,
      JsonAPITestHelper.toAttributeMap(dto));

    assertDoesNotThrow(() -> testRepo.update(docToUpdate));
  }

  @Test
  @WithMockKeycloakUser(groupRole = {GROUP + ":SUPER_USER"}, username = USER)
  public void update_WhenNotCreatedBy_AccessDenied() {
    Item myItem = Item.builder().uuid(UUID.randomUUID()).createdBy("xyz").group(GROUP).build();
    defaultService.create(myItem);

    ItemDto dto = ItemDto.builder().uuid(myItem.getUuid()).createdBy(USER).group(GROUP).build();
    JsonApiDocument docToUpdate = JsonApiDocuments.createJsonApiDocument(myItem.getUuid(), ItemDto.TYPE_NAME,
      JsonAPITestHelper.toAttributeMap(dto));

    assertThrows(AccessDeniedException.class, () -> testRepo.update(docToUpdate));
  }

  @Test
  @WithMockKeycloakUser(groupRole = {GROUP + ":USER"}, username = USER)
  public void delete_WhenOwner_AccessAccepted() {
    Item myItem = Item.builder().uuid(UUID.randomUUID()).createdBy(USER).group(GROUP).build();
    defaultService.create(myItem);
    assertDoesNotThrow(() -> testRepo.delete(myItem.getUuid()));
  }

  @Test
  @WithMockKeycloakUser(groupRole = {GROUP + ":USER"}, username = USER)
  public void delete_WhenNotOwner_AccessDenied() {
    Item myItem = Item.builder().uuid(UUID.randomUUID()).createdBy("xyz").group(GROUP).build();
    defaultService.create(myItem);
    assertThrows(AccessDeniedException.class, () -> testRepo.delete(myItem.getUuid()));
  }

  @TestConfiguration
  static class ObjectOwnerAuthTestConfig {

    @Bean
    @Primary
    public DinaRepositoryV2<ItemDto, Item> testRepo(
      Optional<AuditService> auditService,
      ObjectOwnerAuthorizationService authorizationService,
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
