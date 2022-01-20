package ca.gc.aafc.dina.security;

import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.dto.RelatedEntity;
import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.jpa.BaseDAO;
import ca.gc.aafc.dina.mapper.DinaMapper;
import ca.gc.aafc.dina.repository.DinaRepository;
import ca.gc.aafc.dina.repository.meta.AttributeMetaInfoProvider;
import ca.gc.aafc.dina.service.DefaultDinaService;
import ca.gc.aafc.dina.testsupport.security.WithMockKeycloakUser;
import io.crnk.core.engine.http.HttpRequestContext;
import io.crnk.core.engine.http.HttpRequestContextProvider;
import io.crnk.core.engine.query.QueryContext;
import io.crnk.core.engine.registry.ResourceRegistry;
import io.crnk.core.engine.result.ImmediateResultFactory;
import io.crnk.core.module.ModuleRegistry;
import io.crnk.core.queryspec.QuerySpec;
import io.crnk.core.resource.annotations.JsonApiId;
import io.crnk.core.resource.annotations.JsonApiResource;
import io.crnk.core.resource.list.ResourceList;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hibernate.annotations.NaturalId;
import org.javers.core.metamodel.annotation.PropertyName;
import org.javers.core.metamodel.annotation.TypeName;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.access.prepost.PreAuthorize;
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

@Transactional
@SpringBootTest(classes = {TestDinaBaseApp.class, DinaRepoPermissionMetaTest.TestConfig.class},
  properties = "keycloak.enabled: true")
public class DinaRepoPermissionMetaTest {

  @Inject
  private DinaRepository<TestConfig.ItemDto, TestConfig.Item> testRepo;

  @Inject
  private DefaultDinaService<TestConfig.Item> itemService;

  @BeforeEach
  void setUp() {
    TestConfig.Item persisted = TestConfig.Item.builder()
      .uuid(UUID.randomUUID())
      .group("CNC")
      .build();
    itemService.create(persisted);
  }

  @Test
  @WithMockKeycloakUser(groupRole = {"CNC:DINA_ADMIN"})
  void permissionsTest_WhenHasPermissions_PermissionsReturned() {
    mockHttpHeader(DinaRepository.PERMISSION_META_HEADER_KEY, "true");
    ResourceList<TestConfig.ItemDto> all = testRepo.findAll(new QuerySpec(TestConfig.ItemDto.class));
    all.forEach(result -> {
      MatcherAssert.assertThat(result.getMeta().getPermissions(), Matchers.hasItems("create", "update"));
      MatcherAssert.assertThat(result.getMeta().getPermissionsProvider(), Matchers.is("SpecialAuthServiceUnderTest"));
    });
  }

  @Test
  @WithMockKeycloakUser(groupRole = {"CNC:DINA_ADMIN"})
  void permissionsTest_WhenNoContext_PermissionsNotReturned() {
    ModuleRegistry moduleRegistry = Mockito.mock(ModuleRegistry.class);
    ResourceRegistry resourceRegistry = Mockito.mock(ResourceRegistry.class);
    Mockito.when(resourceRegistry.getLatestVersion()).thenReturn(2);
    Mockito.when(moduleRegistry.getResourceRegistry()).thenReturn(resourceRegistry);
    ImmediateResultFactory resultFactory = new ImmediateResultFactory();
    HttpRequestContextProvider provider = new HttpRequestContextProvider(() -> resultFactory, moduleRegistry);
    testRepo.setHttpRequestContextProvider(provider);
    ResourceList<TestConfig.ItemDto> all = testRepo.findAll(new QuerySpec(TestConfig.ItemDto.class));
    all.forEach(result -> Assertions.assertNull(result.getMeta()));
  }

  @Test
  @WithMockKeycloakUser(groupRole = {"CNC:DINA_ADMIN"})
  void permissionsTest_WhenNoHeader_PermissionsNotReturned() {
    mockHttpHeader("wrong header", "true");
    ResourceList<TestConfig.ItemDto> all = testRepo.findAll(new QuerySpec(TestConfig.ItemDto.class));
    all.forEach(result -> Assertions.assertNull(result.getMeta()));
  }

  @Test
  @WithMockKeycloakUser(groupRole = {"InvalidGroup:STAFF"})
  void permissionsTest_WhenNoPermissions_PermissionsNotReturned() {
    mockHttpHeader(DinaRepository.PERMISSION_META_HEADER_KEY, "true");
    ResourceList<TestConfig.ItemDto> all = testRepo.findAll(new QuerySpec(TestConfig.ItemDto.class));
    all.forEach(result -> MatcherAssert.assertThat(result.getMeta().getPermissions(), Matchers.empty()));
  }

  private void mockHttpHeader(String header, String value) {
    ModuleRegistry moduleRegistry = Mockito.mock(ModuleRegistry.class);
    ResourceRegistry resourceRegistry = Mockito.mock(ResourceRegistry.class);
    Mockito.when(resourceRegistry.getLatestVersion()).thenReturn(2);
    Mockito.when(moduleRegistry.getResourceRegistry()).thenReturn(resourceRegistry);

    ImmediateResultFactory resultFactory = new ImmediateResultFactory();
    HttpRequestContextProvider provider = new HttpRequestContextProvider(() -> resultFactory, moduleRegistry);

    HttpRequestContext context = Mockito.mock(HttpRequestContext.class);
    Mockito.when(context.getBaseUrl()).thenReturn("http://test");
    Mockito.when(context.getRequestHeader(header)).thenReturn(value);
    QueryContext queryContext = new QueryContext();
    Mockito.when(context.getQueryContext()).thenReturn(queryContext);

    provider.onRequestStarted(context);

    testRepo.setHttpRequestContextProvider(provider);
  }

  @TestConfiguration
  @EntityScan(basePackageClasses = TestConfig.class)
  static class TestConfig {

    @Bean
    @Primary
    public DinaRepository<ItemDto, Item> testRepo(
      SpecialAuthServiceUnderTest authorizationService,
      BuildProperties buildProperties,
      DefaultDinaService<Item> defaultService
    ) {
      DinaMapper<ItemDto, Item> dinaMapper = new DinaMapper<>(
        ItemDto.class);
      return new DinaRepository<>(
        defaultService,
        authorizationService,
        Optional.empty(),
        dinaMapper,
        ItemDto.class,
        Item.class,
        null,
        null,
        buildProperties);
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
    @EqualsAndHashCode(callSuper = true)
    @JsonApiResource(type = ItemDto.TYPE_NAME)
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @RelatedEntity(Item.class)
    @TypeName(ItemDto.TYPE_NAME)
    public static class ItemDto extends AttributeMetaInfoProvider {
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
      public ItemService(@NonNull BaseDAO baseDAO, @NonNull SmartValidator sv) {
        super(baseDAO, sv);
      }
    }

    @Service
    public static class SpecialAuthServiceUnderTest extends PermissionAuthorizationService {

      @Override
      @PreAuthorize("hasGroupPermission(@currentUser, #entity)")
      public void authorizeCreate(Object entity) {

      }

      @Override
      public void authorizeRead(Object entity) {

      }

      @Override
      @PreAuthorize("hasDinaRole(@currentUser, 'DINA_ADMIN')")
      public void authorizeUpdate(Object entity) {

      }

      @Override
      @PreAuthorize("hasDinaRole(@currentUser, 'STUDENT')")
      public void authorizeDelete(Object entity) {

      }

      @Override
      public String getName() {
        return "SpecialAuthServiceUnderTest";
      }
    }
  }

}
