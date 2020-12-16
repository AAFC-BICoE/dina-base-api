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
import java.util.Optional;
import java.util.Properties;

@SpringBootTest(
  properties = {"dev-user.enabled: true", "keycloak.enabled: false"},
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(WarningInfoProviderRestIT.TestConfig.class)
public class WarningInfoProviderRestIT extends BaseRestAssuredTest {

  public static final String KEY = "name_to_long";
  public static final String VALUE = "name to long";

  protected WarningInfoProviderRestIT() {
    super("thing");
  }

  @Test
  void metaInfo_ReturnedInResponse() {
    ThingDTO dto = ThingDTO.builder().name("new name").build();
    ValidatableResponse response = sendPost(JsonAPITestHelper.toJsonAPIMap(
      "thing", JsonAPITestHelper.toAttributeMap(dto), null, null));
    response.body("data.meta." + KEY, Matchers.equalTo(VALUE));
  }

  @TestConfiguration
  @EntityScan(basePackageClasses = WarningInfoProviderRestIT.class)
  static class TestConfig {
    @Bean
    public DinaRepository<ThingDTO, Thing> projectRepo(
      BaseDAO baseDAO,
      DinaFilterResolver filterResolver
    ) {
      return new WarningRepo(baseDAO, filterResolver, resource -> {
        WarningInfoProvider.DinaJsonMetaInfo meta = WarningInfoProvider.DinaJsonMetaInfo.builder()
          .build();
        meta.setProperties(KEY, VALUE);
        resource.setMeta(meta);
      });
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = false)
  @JsonApiResource(type = "thing")
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @RelatedEntity(Thing.class)
  public static class ThingDTO extends WarningInfoProvider {
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
  public static class WarningRepo extends DinaRepository<ThingDTO, Thing> {

    private final WarningInfoHandler<ThingDTO> handler;

    public WarningRepo(
      BaseDAO baseDAO,
      DinaFilterResolver filterResolver,
      WarningInfoHandler<ThingDTO> handler
    ) {
      super(
        new DefaultDinaService<>(baseDAO),
        Optional.empty(),
        Optional.empty(),
        new DinaMapper<>(ThingDTO.class),
        ThingDTO.class,
        Thing.class,
        filterResolver,
        null,
        new BuildProperties(new Properties()));
      this.handler = handler;
    }

    @Override
    public <S extends ThingDTO> S create(S resource) {
      S persisted = super.create(resource);
      handler.loadWarnings(persisted);
      return persisted;
    }

    @Override
    public <S extends ThingDTO> S save(S resource) {
      S persisted = super.save(resource);
      handler.loadWarnings(persisted);
      return persisted;
    }
  }

}
