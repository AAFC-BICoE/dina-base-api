package ca.gc.aafc.dina.repository.meta;

import ca.gc.aafc.dina.dto.RelatedEntity;
import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.filter.DinaFilterResolver;
import ca.gc.aafc.dina.jpa.BaseDAO;
import ca.gc.aafc.dina.mapper.DinaMapper;
import ca.gc.aafc.dina.repository.DinaRepository;
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
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Repository;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;

@SpringBootTest(
  properties = {"dev-user.enabled: true", "keycloak.enabled: false"},
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(DinaJsonMetaInfoProviderRestIT.TestConfig.class)
public class DinaJsonMetaInfoProviderRestIT extends BaseRestAssuredTest {

  public static final String KEY = "Warnings";
  public static final String VALUE_1 = "name to long";
  public static final String VALUE_2 = "duplicate name detected";

  protected DinaJsonMetaInfoProviderRestIT() {
    super("thing");
  }

  @Test
  void metaInfo_ReturnedInResponse() {
    ThingDTO dto = ThingDTO.builder().name("new name").build();
    ValidatableResponse response = sendPost(JsonAPITestHelper.toJsonAPIMap(
      "thing", JsonAPITestHelper.toAttributeMap(dto), null, null));
    response.body("data.meta." + KEY, Matchers.contains(VALUE_1, VALUE_2));
  }

  @TestConfiguration
  @EntityScan(basePackageClasses = DinaJsonMetaInfoProviderRestIT.class)
  static class TestConfig {
    @Bean
    public DinaRepository<ThingDTO, Thing> projectRepo(
      BaseDAO baseDAO,
      DinaFilterResolver filterResolver
    ) {
      return new DinaMetaInfoRepo<>(
        baseDAO,
        ThingDTO.class,
        Thing.class,
        filterResolver,
        thingDTO -> DinaJsonMetaInfoProvider.DinaJsonMetaInfo.builder()
          .properties(Map.of(KEY, List.of(VALUE_1, VALUE_2).toArray()))
          .build());
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = false)
  @JsonApiResource(type = "thing")
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @RelatedEntity(Thing.class)
  public static class ThingDTO extends DinaJsonMetaInfoProvider {
    @JsonApiId
    private Integer id;
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
  public static class DinaMetaInfoRepo<D extends DinaJsonMetaInfoProvider, E extends DinaEntity>
    extends DinaRepository<D, E> {

    private final Function<D, DinaJsonMetaInfoProvider.DinaJsonMetaInfo> handler;

    public DinaMetaInfoRepo(
      BaseDAO baseDAO,
      Class<D> resourceClass,
      Class<E> entityClass,
      DinaFilterResolver filterResolver,
      Function<D, DinaJsonMetaInfoProvider.DinaJsonMetaInfo> handler
    ) {
      super(
        new DefaultDinaService<>(baseDAO),
        Optional.empty(),
        Optional.empty(),
        new DinaMapper<>(resourceClass),
        resourceClass,
        entityClass,
        filterResolver,
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
