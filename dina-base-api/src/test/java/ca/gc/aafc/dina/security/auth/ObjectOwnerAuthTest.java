package ca.gc.aafc.dina.security.auth;

import org.javers.core.metamodel.annotation.PropertyName;
import org.javers.core.metamodel.annotation.TypeName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.validation.SmartValidator;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.dto.RelatedEntity;
import ca.gc.aafc.dina.entity.Item;
import ca.gc.aafc.dina.jpa.BaseDAO;
import ca.gc.aafc.dina.mapper.DinaMapper;
import ca.gc.aafc.dina.repository.DinaRepository;
import ca.gc.aafc.dina.repository.DinaRepositoryIT;
import ca.gc.aafc.dina.service.AuditService;
import ca.gc.aafc.dina.service.DefaultDinaService;
import ca.gc.aafc.dina.testsupport.PostgresTestContainerInitializer;
import ca.gc.aafc.dina.testsupport.security.WithMockKeycloakUser;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.crnk.core.resource.annotations.JsonApiId;
import io.crnk.core.resource.annotations.JsonApiResource;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import javax.inject.Inject;
import javax.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Transactional
@SpringBootTest(classes = {TestDinaBaseApp.class, ObjectOwnerAuthTest.DinaAdminOnlyTestConfig.class},
  properties = "keycloak.enabled: true")
@ContextConfiguration(initializers = { PostgresTestContainerInitializer.class })
public class ObjectOwnerAuthTest {

  private static final String GROUP = "group";
  private static final String USER = "myuser";

  @Inject
  private DefaultDinaService<Item> defaultService;

  @Inject
  private DinaRepository<ItemDto, Item> testRepo;

  @Test
  @WithMockKeycloakUser(groupRole = {GROUP + ":SUPER_USER"}, username = USER)
  public void create_WhenNotCreatedBy_AccessDenied() {
    ItemDto dto = ItemDto.builder().uuid(UUID.randomUUID()).createdBy("xyz").group(GROUP).build();
    assertThrows(AccessDeniedException.class, () -> testRepo.create(dto));
  }

  @Test
  @WithMockKeycloakUser(groupRole = {GROUP + ":SUPER_USER"}, username = USER)
  void update_WhenCreatedBy_AccessAccepted() {
    Item myItem = Item.builder().uuid(UUID.randomUUID()).createdBy(USER).group(GROUP).build();
    defaultService.create(myItem);
    ItemDto dto = ItemDto.builder().uuid(myItem.getUuid()).createdBy(USER).group(GROUP).build();
    assertDoesNotThrow(() -> testRepo.save(dto));
  }

  @Test
  @WithMockKeycloakUser(groupRole = {GROUP + ":SUPER_USER"}, username = USER)
  public void update_WhenNotCreatedBy_AccessDenied() {
    Item myItem = Item.builder().uuid(UUID.randomUUID()).createdBy("xyz").group(GROUP).build();
    defaultService.create(myItem);

    ItemDto dto = ItemDto.builder().uuid(myItem.getUuid()).createdBy(USER).group(GROUP).build();
    assertThrows(AccessDeniedException.class, () -> testRepo.save(dto));
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
  @EntityScan(basePackageClasses = ObjectOwnerAuthTest.class)
  static class DinaAdminOnlyTestConfig {

    @Bean
    @Primary
    public DinaRepository<ItemDto, Item> testRepo(
      DinaRepositoryIT.DinaPersonService service,
      Optional<AuditService> auditService,
      ObjectOwnerAuthorizationService authorizationService,
      BuildProperties buildProperties,
      DefaultDinaService<Item> defaultService, ObjectMapper objMapper
    ) {
      DinaMapper<ItemDto, Item> dinaMapper = new DinaMapper<>(ItemDto.class);
      return new DinaRepository<>(
        defaultService,
        authorizationService,
        auditService,
        dinaMapper,
        ItemDto.class,
        Item.class,
        null,
        null,
        buildProperties, objMapper);
    }

  }

  @Data
  @JsonApiResource(type = ItemDto.TYPE_NAME)
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @RelatedEntity(Item.class)
  @TypeName(ItemDto.TYPE_NAME)
  public static class ItemDto {
    private static final String TYPE_NAME = "item";
    @JsonApiId
    @org.javers.core.metamodel.annotation.Id
    @PropertyName("id")
    private UUID uuid;
    private String group;
    private String createdBy;
    private OffsetDateTime createdOn;
  }

  @Service
  public static class ItemService extends DefaultDinaService<Item> {
    public ItemService(@NonNull BaseDAO baseDAO, SmartValidator sv) {
      super(baseDAO, sv);
    }
  }

}
