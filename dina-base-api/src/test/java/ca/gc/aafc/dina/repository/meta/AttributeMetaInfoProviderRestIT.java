package ca.gc.aafc.dina.repository.meta;

import ca.gc.aafc.dina.dto.RelatedEntity;
import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.jpa.BaseDAO;
import ca.gc.aafc.dina.mapper.DinaMapper;
import ca.gc.aafc.dina.repository.DinaRepository;
import ca.gc.aafc.dina.security.spring.SecurityChecker;
import ca.gc.aafc.dina.security.AllowAllAuthorizationService;
import ca.gc.aafc.dina.service.DefaultDinaService;
import ca.gc.aafc.dina.testsupport.BaseRestAssuredTest;
import ca.gc.aafc.dina.testsupport.jsonapi.JsonAPITestHelper;
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
import org.hibernate.annotations.NaturalId;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.validation.SmartValidator;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;

import java.time.OffsetDateTime;
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
public class AttributeMetaInfoProviderRestIT extends BaseRestAssuredTest {

  public static final String KEY = "duplicate_found";
  public static final String VALUE = "A record with title Rails is Omakase already exists";

  protected AttributeMetaInfoProviderRestIT() {
    super("thing");
  }

  @Test
  void metaInfo_ReturnedInResponse() {
    ThingDTO dto = ThingDTO.builder().name("new name").build();
    ValidatableResponse response = sendPost(JsonAPITestHelper.toJsonAPIMap(
      "thing", JsonAPITestHelper.toAttributeMap(dto), null, null));
    response.body("data.meta.warnings", Matchers.hasEntry(KEY, VALUE));
  }

  @TestConfiguration
  @EntityScan(basePackageClasses = AttributeMetaInfoProviderRestIT.class)
  static class TestConfig {
    @Bean
    public DinaRepository<ThingDTO, Thing> projectRepo(
      BaseDAO baseDAO,
      ThingDinaService dinaService,
      SecurityChecker securityChecker
    ) {
      Map<String, Object> warnings = new HashMap<>();
      warnings.put(KEY, VALUE);
      return new DinaMetaInfoRepo<>(
        baseDAO,
        dinaService,
        ThingDTO.class,
        Thing.class,
        thingDTO -> AttributeMetaInfoProvider.DinaJsonMetaInfo.builder()
          .warnings(warnings)
          .build(), securityChecker);
    }

    @Service
    public class ThingDinaService extends DefaultDinaService<Thing> {
  
      public ThingDinaService(@NonNull BaseDAO baseDAO, SmartValidator sv) {
        super(baseDAO, sv);
      }
  
      @Override
      protected void preCreate(Thing entity) {
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
  @RelatedEntity(Thing.class)
  public static class ThingDTO extends AttributeMetaInfoProvider {
    @JsonApiId
    private Integer id;
    private UUID uuid;
    private String name;
  }

  @Data
  @Entity
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Thing implements DinaEntity {
    @Id
    @GeneratedValue
    private Integer id;

    @NaturalId
    @NotNull
    private UUID uuid;

    private String name;

    @Override
    public String getCreatedBy() {
      return null;
    }

    @Override
    public OffsetDateTime getCreatedOn() {
      return null;
    }
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
      @NonNull SecurityChecker securityChecker
    ) {
      super(
        dinaService,
        new AllowAllAuthorizationService(),
        Optional.empty(),
        new DinaMapper<>(resourceClass),
        resourceClass,
        entityClass,
        securityChecker, null,
        null,
        new BuildProperties(new Properties()));
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
