package ca.gc.aafc.dina.security;

import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.TestDinaBaseApp.DinaPersonService;
import ca.gc.aafc.dina.dto.RelatedEntity;
import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.jpa.BaseDAO;
import ca.gc.aafc.dina.mapper.DinaMapper;
import ca.gc.aafc.dina.repository.DinaRepository;
import ca.gc.aafc.dina.repository.DinaRepositoryIT;
import ca.gc.aafc.dina.service.AuditService;
import ca.gc.aafc.dina.service.DefaultDinaService;
import ca.gc.aafc.dina.testsupport.security.WithMockKeycloakUser;
import io.crnk.core.resource.annotations.JsonApiId;
import io.crnk.core.resource.annotations.JsonApiResource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.hibernate.annotations.NaturalId;
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
import org.springframework.validation.SmartValidator;

import javax.inject.Inject;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
@SpringBootTest(classes = {TestDinaBaseApp.class, DinaAdminOnlyAuthTest.DinaAdminOnlyTestConfig.class},
  properties = "keycloak.enabled: true")
public class DinaAdminOnlyAuthTest {

  @Inject
  private DefaultDinaService<Item> defaultService;

  @Inject
  private DinaAdminOnlyAuthorizationService authorizationService;

  public Item persisted;

  @Inject
  private DinaRepository<ItemDto, Item> testRepo;

  @BeforeEach
  void setUp() {
    persisted = Item.builder().uuid(UUID.randomUUID()).group("group").build();
    defaultService.create(persisted);
  }

  @Test
  @WithMockKeycloakUser(groupRole = {"CNC:DINA_ADMIN"})
  public void create_WhenAdmin_CreatesObject() {
    ItemDto dto = ItemDto.builder().uuid(UUID.randomUUID()).group("g").build();
    ItemDto result = testRepo.create(dto);
    assertNotNull(result.getUuid());
  }

  @Test
  @WithMockKeycloakUser(groupRole = {"CNC:CNC:COLLECTION_MANAGER", "GNG:CNC:STAFF", "BNB:CNC:STUDENT"})
  public void create_WhenNotAdmin_AccessDenied() {
    ItemDto dto = ItemDto.builder().uuid(UUID.randomUUID()).group("g").build();
    assertThrows(AccessDeniedException.class, () -> testRepo.create(dto));
  }

  @Test
  @WithMockKeycloakUser(groupRole = {"CNC:DINA_ADMIN"})
  void update_WhenAdmin_AccessAccepted() {
    assertDoesNotThrow(() -> testRepo.save(ItemDto.builder().uuid(persisted.uuid).build()));
  }

  @Test
  @WithMockKeycloakUser(groupRole = {"CNC:CNC:COLLECTION_MANAGER", "GNG:CNC:STAFF", "BNB:CNC:STUDENT"})
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
  @WithMockKeycloakUser(groupRole = {"CNC:CNC:COLLECTION_MANAGER", "GNG:CNC:STAFF", "BNB:CNC:STUDENT"})
  public void delete_WhenNotAdmin_AccessDenied() {
    assertThrows(AccessDeniedException.class, () -> testRepo.delete(persisted.getUuid()));
  }

  @Test
  void getName() {
    Assertions.assertEquals("DinaAdminOnlyAuthorizationService", authorizationService.getName());
  }

  @TestConfiguration
  @EntityScan(basePackageClasses = DinaAdminOnlyAuthTest.class)
  static class DinaAdminOnlyTestConfig {

    @Bean
    @Primary
    public DinaRepository<ItemDto, Item> testRepo(
      DinaPersonService service,
      Optional<AuditService> auditService,
      DinaAdminOnlyAuthorizationService authorizationService,
      BuildProperties buildProperties,
      BaseDAO baseDao,
      DefaultDinaService<Item> defaultService
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
        buildProperties);
    }

  }

  @Data
  @Entity
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Item implements DinaEntity {
    private String createdBy;
    private OffsetDateTime createdOn;
    @Column(name = "group_name")
    private String group;
    @Id
    @GeneratedValue
    private Integer id;
    @NaturalId
    private UUID uuid;
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
