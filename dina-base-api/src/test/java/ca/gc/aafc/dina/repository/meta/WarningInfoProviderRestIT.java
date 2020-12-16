package ca.gc.aafc.dina.repository.meta;

import ca.gc.aafc.dina.dto.RelatedEntity;
import ca.gc.aafc.dina.dto.WarningInfoProvider;
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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
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

  protected WarningInfoProviderRestIT() {
    super("thing");
  }

  @Test
  void test() {
    ThingDTO dto = ThingDTO.builder().name("new name").build();
    sendPost(JsonAPITestHelper.toJsonAPIMap(
      "thing", JsonAPITestHelper.toAttributeMap(dto), null, null))
      .log().all(true);
  }

  @TestConfiguration
  @EntityScan(basePackageClasses = WarningInfoProviderRestIT.class)
  static class TestConfig {
    @Bean
    public DinaRepository<ThingDTO, Thing> projectRepo(
      BaseDAO baseDAO,
      DinaFilterResolver filterResolver
    ) {
      return new MockRepo(baseDAO, filterResolver);
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
  public static class MockRepo extends DinaRepository<ThingDTO, Thing> {

    public MockRepo(BaseDAO baseDAO, DinaFilterResolver filterResolver) {
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
    }

    @Override
    public <S extends ThingDTO> S create(S resource) {
      S s = super.create(resource);
      s.setMeta(WarningInfoProvider.WarningMetaInfo.builder()
        .key("name_to_long")
        .value("name is to long").build());
      return s;
    }
  }

}
