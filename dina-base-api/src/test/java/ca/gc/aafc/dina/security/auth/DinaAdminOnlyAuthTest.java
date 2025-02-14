package ca.gc.aafc.dina.security.auth;

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
import com.fasterxml.jackson.databind.ObjectMapper;
import io.crnk.core.resource.annotations.JsonApiId;
import io.crnk.core.resource.annotations.JsonApiResource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.javers.core.metamodel.annotation.PropertyName;
import org.javers.core.metamodel.annotation.TypeName;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
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

import javax.inject.Inject;
import javax.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
@SpringBootTest(classes = {TestDinaBaseApp.class, DinaAdminOnlyAuthTest.DinaAdminOnlyTestConfig.class},
  properties = "keycloak.enabled: true")
@ContextConfiguration(initializers = { PostgresTestContainerInitializer.class })
public class DinaAdminOnlyAuthTest {

  @Inject
  private DefaultDinaService<Item> defaultService;

  @Inject
  private DinaAdminCUDAuthorizationService authorizationService;

  public Item persisted;

  @Inject
  private DinaRepository<ItemDto, Item> testRepo;

  @BeforeEach
  void setUp() {
    persisted = Item.builder().uuid(UUID.randomUUID()).group("group").build();
    defaultService.create(persisted);
  }

  @Test
  @WithMockKeycloakUser(adminRole = {"DINA_ADMIN"})
  public void create_WhenAdmin_CreatesObject() {
    ItemDto dto = ItemDto.builder().uuid(UUID.randomUUID()).group("g").build();
    ItemDto result = testRepo.create(dto);
    assertNotNull(result.getUuid());
  }

  @Test
  @WithMockKeycloakUser(groupRole = {"CNC:SUPER_USER", "CNC:USER", "CNC:GUEST"})
  public void create_WhenNotAdmin_AccessDenied() {
    ItemDto dto = ItemDto.builder().uuid(UUID.randomUUID()).group("g").build();
    assertThrows(AccessDeniedException.class, () -> testRepo.create(dto));
  }

  @Test
  @WithMockKeycloakUser(groupRole = {"CNC:DINA_ADMIN"})
  void update_WhenAdmin_AccessAccepted() {
    assertDoesNotThrow(() -> testRepo.save(ItemDto.builder().uuid(persisted.getUuid()).build()));
  }

  @Test
  @WithMockKeycloakUser(groupRole = {"CNC:SUPER_USER", "CNC:USER", "CNC:GUEST"})
  public void update_WhenNotAdmin_AccessDenied() {
    ItemDto dto = ItemDto.builder().uuid(UUID.randomUUID()).group("g").build();
    assertThrows(AccessDeniedException.class, () -> testRepo.save(dto));
  }

  @Test
  @WithMockKeycloakUser(groupRole = {"CNC:DINA_ADMIN"})
  public void delete_WhenAdmin_AccessAccepted() {
    assertDoesNotThrow(() -> testRepo.delete(persisted.getUuid()));
  }

  @Test
  @WithMockKeycloakUser(groupRole = {"CNC:SUPER_USER", "CNC:USER", "CNC:GUEST"})
  public void delete_WhenNotAdmin_AccessDenied() {
    assertThrows(AccessDeniedException.class, () -> testRepo.delete(persisted.getUuid()));
  }

  @Test
  void getName() {
    Assertions.assertEquals("DinaAdminCUDAuthorizationService", authorizationService.getName());
  }

  @TestConfiguration
  @EntityScan(basePackageClasses = DinaAdminOnlyAuthTest.class)
  static class DinaAdminOnlyTestConfig {

    @Bean
    @Primary
    public DinaRepository<ItemDto, Item> testRepo(
      DinaRepositoryIT.DinaPersonService service,
      Optional<AuditService> auditService,
      DinaAdminCUDAuthorizationService authorizationService,
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
