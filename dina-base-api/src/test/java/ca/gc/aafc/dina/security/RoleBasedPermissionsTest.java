package ca.gc.aafc.dina.security;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ca.gc.aafc.dina.TestConfiguration;
import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.filter.DinaFilterResolver;
import ca.gc.aafc.dina.jpa.BaseDAO;
import ca.gc.aafc.dina.mapper.DinaMapper;
import ca.gc.aafc.dina.repository.DinaRepository;
import ca.gc.aafc.dina.security.RoleBasedPermissionsTest.RoleTestConfig;
import ca.gc.aafc.dina.service.DinaAuthorizationService;
import ca.gc.aafc.dina.service.DinaService;
import ca.gc.aafc.dina.service.RoleAuthorizationService;
import io.crnk.core.resource.annotations.JsonApiId;
import io.crnk.core.resource.annotations.JsonApiResource;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@SpringBootTest(classes = { TestConfiguration.class, RoleTestConfig.class })
public class RoleBasedPermissionsTest {

  @Configuration
  @EntityScan(basePackageClasses = RoleBasedPermissionsTest.class)
  static class RoleTestConfig {

    @Bean
    public DinaRepository<Dto, TestEntity> repo(
      BaseDAO baseDAO,
      DinaFilterResolver filterResolver,
      DinaAuthenticatedUser user
    ) {
      return new Repo(
        baseDAO,
        Optional.of(new RoleAuthorizationService(DinaRole.COLLECTION_MANAGER, user)),
        filterResolver);
    }
  }

  @Inject
  private DinaRepository<Dto, TestEntity> dinaRepository;

  @Test
  public void name() {
    assertNotNull(dinaRepository);
    dinaRepository.create(new Dto());
  }

  @Data
  @NoArgsConstructor
  @Entity
  public static class TestEntity implements DinaEntity {
    @Id
    @GeneratedValue
    private Integer id;
    private UUID uuid;
  }

  @Data
  @JsonApiResource(type = "Dto")
  public static class Dto {
    @JsonApiId
    private UUID uuid;

  }

  static class Repo extends DinaRepository<Dto, TestEntity> {

    public Repo(
      BaseDAO baseDAO,
      Optional<DinaAuthorizationService> authorizationService,
      DinaFilterResolver filterResolver
    ) {
      super(
        new Service(baseDAO),
        authorizationService,
        new DinaMapper<>(Dto.class), 
        Dto.class,
        TestEntity.class,
        filterResolver);
    }

  }

  static class Service extends DinaService<TestEntity> {

    public Service(@NonNull BaseDAO baseDAO) {
      super(baseDAO);
    }

  }

}
