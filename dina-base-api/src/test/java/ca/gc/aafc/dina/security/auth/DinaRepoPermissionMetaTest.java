package ca.gc.aafc.dina.security.auth;

import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.dto.ItemDto;
import ca.gc.aafc.dina.dto.RelatedEntity;
import ca.gc.aafc.dina.entity.Item;
import ca.gc.aafc.dina.jpa.BaseDAO;
import ca.gc.aafc.dina.repository.DinaRepositoryV2;
import ca.gc.aafc.dina.repository.meta.AttributeMetaInfoProvider;
import ca.gc.aafc.dina.service.DefaultDinaService;
import ca.gc.aafc.dina.testsupport.PostgresTestContainerInitializer;
import ca.gc.aafc.dina.testsupport.security.WithMockKeycloakUser;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.validation.SmartValidator;

import javax.inject.Inject;
import javax.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Transactional
@SpringBootTest(classes = {TestDinaBaseApp.class, DinaRepoPermissionMetaTest.TestConfig.class},
  properties = "keycloak.enabled: true")
@ContextConfiguration(initializers = { PostgresTestContainerInitializer.class })
public class DinaRepoPermissionMetaTest {

  @Inject
  private DinaRepositoryV2<ItemDto, Item> testRepo;

  @Inject
  private DefaultDinaService<Item> itemService;

  @BeforeEach
  void setUp() {
    Item persisted = Item.builder()
      .uuid(UUID.randomUUID())
      .group("CNC")
      .build();
    itemService.create(persisted);
  }

//  @Test
//  @WithMockKeycloakUser(groupRole = {"CNC:DINA_ADMIN"})
//  void permissionsTest_WhenHasPermissions_PermissionsReturned() {
//    mockHttpHeader(DinaRepository.PERMISSION_META_HEADER_KEY, "true");
//    ResourceList<TestConfig.ItemDto> all = testRepo.findAll(new QuerySpec(TestConfig.ItemDto.class));
//    all.forEach(result -> {
//      MatcherAssert.assertThat(result.getMeta().getPermissions(), Matchers.hasItems("create", "update"));
//      MatcherAssert.assertThat(result.getMeta().getPermissionsProvider(), Matchers.is("SpecialAuthServiceUnderTest"));
//    });
//  }
//
//  @Test
//  @WithMockKeycloakUser(groupRole = {"CNC:DINA_ADMIN"})
//  void permissionsTest_WhenNoContext_PermissionsNotReturned() {
//    ModuleRegistry moduleRegistry = Mockito.mock(ModuleRegistry.class);
//    ResourceRegistry resourceRegistry = Mockito.mock(ResourceRegistry.class);
//    Mockito.when(resourceRegistry.getLatestVersion()).thenReturn(2);
//    Mockito.when(moduleRegistry.getResourceRegistry()).thenReturn(resourceRegistry);
//    ImmediateResultFactory resultFactory = new ImmediateResultFactory();
//    HttpRequestContextProvider provider = new HttpRequestContextProvider(() -> resultFactory, moduleRegistry);
//    testRepo.setHttpRequestContextProvider(provider);
//    ResourceList<TestConfig.ItemDto> all = testRepo.findAll(new QuerySpec(TestConfig.ItemDto.class));
//    all.forEach(result -> Assertions.assertNull(result.getMeta()));
//  }
//
//  @Test
//  @WithMockKeycloakUser(groupRole = {"CNC:DINA_ADMIN"})
//  void permissionsTest_WhenNoHeader_PermissionsNotReturned() {
//    mockHttpHeader("wrong header", "true");
//    ResourceList<TestConfig.ItemDto> all = testRepo.findAll(new QuerySpec(TestConfig.ItemDto.class));
//    all.forEach(result -> Assertions.assertNull(result.getMeta()));
//  }
//
//  @Test
//  @WithMockKeycloakUser(groupRole = {"InvalidGroup:USER"})
//  void permissionsTest_WhenNoPermissions_PermissionsNotReturned() {
//    mockHttpHeader(DinaRepository.PERMISSION_META_HEADER_KEY, "true");
//    ResourceList<TestConfig.ItemDto> all = testRepo.findAll(new QuerySpec(TestConfig.ItemDto.class));
//    all.forEach(result -> MatcherAssert.assertThat(result.getMeta().getPermissions(), Matchers.empty()));
//  }
//
//  private void mockHttpHeader(String header, String value) {
//    ModuleRegistry moduleRegistry = Mockito.mock(ModuleRegistry.class);
//    ResourceRegistry resourceRegistry = Mockito.mock(ResourceRegistry.class);
//    Mockito.when(resourceRegistry.getLatestVersion()).thenReturn(2);
//    Mockito.when(moduleRegistry.getResourceRegistry()).thenReturn(resourceRegistry);
//
//    ImmediateResultFactory resultFactory = new ImmediateResultFactory();
//    HttpRequestContextProvider provider = new HttpRequestContextProvider(() -> resultFactory, moduleRegistry);
//
//    HttpRequestContext context = Mockito.mock(HttpRequestContext.class);
//    Mockito.when(context.getBaseUrl()).thenReturn("http://test");
//    Mockito.when(context.getRequestHeader(header)).thenReturn(value);
//    QueryContext queryContext = new QueryContext();
//    Mockito.when(context.getQueryContext()).thenReturn(queryContext);
//
//    provider.onRequestStarted(context);
//
//    testRepo.setHttpRequestContextProvider(provider);
//  }

  @TestConfiguration
 // @EntityScan(basePackageClasses = TestConfig.class)
  static class TestConfig {
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
      @PreAuthorize("hasDinaRole(@currentUser, 'GUEST')")
      public void authorizeDelete(Object entity) {

      }

      @Override
      public String getName() {
        return "SpecialAuthServiceUnderTest";
      }
    }
  }
}
