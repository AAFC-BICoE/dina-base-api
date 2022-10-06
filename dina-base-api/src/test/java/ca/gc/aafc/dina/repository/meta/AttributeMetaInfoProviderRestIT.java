package ca.gc.aafc.dina.repository.meta;

import ca.gc.aafc.dina.dto.RelatedEntity;
import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.entity.Item;
import ca.gc.aafc.dina.jpa.BaseDAO;
import ca.gc.aafc.dina.mapper.DinaMapper;
import ca.gc.aafc.dina.repository.DinaRepository;
import ca.gc.aafc.dina.security.AllowAllAuthorizationService;
import ca.gc.aafc.dina.service.DefaultDinaService;
import ca.gc.aafc.dina.testsupport.BaseRestAssuredTest;
import ca.gc.aafc.dina.testsupport.PostgresTestContainerInitializer;
import ca.gc.aafc.dina.testsupport.jsonapi.JsonAPITestHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.crnk.core.resource.annotations.JsonApiId;
import io.crnk.core.resource.annotations.JsonApiResource;
import io.restassured.response.ValidatableResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.validation.SmartValidator;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.function.Function;

@SpringBootTest(
  properties = {"dev-user.enabled: true", "keycloak.enabled: false"},
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(AttributeMetaInfoProviderRestIT.TestConfig.class)
@ContextConfiguration(initializers = { PostgresTestContainerInitializer.class })
public class AttributeMetaInfoProviderRestIT extends BaseRestAssuredTest {

  public static final String KEY = "duplicate_found";
  public static final String VALUE = "A record with title Rails is Omakase already exists";

  protected AttributeMetaInfoProviderRestIT() {
    super("thing");
  }

  @Test
  void metaInfo_ReturnedInResponse() {
    ThingDTO dto = ThingDTO.builder().group("new name").build();
    ValidatableResponse response = sendPost(JsonAPITestHelper.toJsonAPIMap(
      "thing", JsonAPITestHelper.toAttributeMap(dto), null, null));
    response.body("data.meta.warnings", Matchers.hasEntry(KEY, VALUE));
  }

  @TestConfiguration
  @EntityScan(basePackageClasses = AttributeMetaInfoProviderRestIT.class)
  static class TestConfig {
    @Bean
    public DinaRepository<ThingDTO, Item> projectRepo(
      BaseDAO baseDAO,
      ItemDinaService dinaService, ObjectMapper objMapper
    ) {
      Map<String, Object> warnings = new HashMap<>();
      warnings.put(KEY, VALUE);
      return new DinaMetaInfoRepo<>(
        baseDAO,
        dinaService,
        ThingDTO.class, Item.class,
        thingDTO -> AttributeMetaInfoProvider.DinaJsonMetaInfo.builder()
          .warnings(warnings)
          .build(), objMapper);
    }

    @Service
    public static class ItemDinaService extends DefaultDinaService<Item> {
  
      public ItemDinaService(@NonNull BaseDAO baseDAO, SmartValidator sv) {
        super(baseDAO, sv);
      }
  
      @Override
      protected void preCreate(Item entity) {
        entity.setUuid(UUID.randomUUID());
      }
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = false)
  @JsonApiResource(type = "thing")
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @RelatedEntity(Item.class)
  public static class ThingDTO extends AttributeMetaInfoProvider {
    @JsonApiId
    private Integer id;
    private UUID uuid;
    private String group;
  }

  @Repository
  public static class DinaMetaInfoRepo<D extends AttributeMetaInfoProvider, E extends DinaEntity>
    extends DinaRepository<D, E> {

    private final Function<D, AttributeMetaInfoProvider.DinaJsonMetaInfo> handler;

    public DinaMetaInfoRepo(
      BaseDAO baseDAO,
      DefaultDinaService<E> dinaService,
      Class<D> resourceClass,
      Class<E> entityClass,
      Function<D, AttributeMetaInfoProvider.DinaJsonMetaInfo> handler,
      ObjectMapper objMapper
    ) {
      super(
        dinaService,
        new AllowAllAuthorizationService(),
        Optional.empty(),
        new DinaMapper<>(resourceClass),
        resourceClass,
        entityClass,
        null,
        null,
        new BuildProperties(new Properties()), objMapper);
      this.handler = handler;
    }

    @Override
    public <S extends D> S create(S resource) {
      S persisted = super.create(resource);
      persisted.setMeta(handler.apply(persisted));
      return persisted;
    }

    @Override
    public <S extends D> S save(S resource) {
      S persisted = super.save(resource);
      persisted.setMeta(handler.apply(persisted));
      return persisted;
    }
  }

}
