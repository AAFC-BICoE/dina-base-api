package ca.gc.aafc.dina.security;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import javax.inject.Inject;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.mockito.Answers;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import ca.gc.aafc.dina.TestConfiguration;
import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.filter.DinaFilterResolver;
import ca.gc.aafc.dina.jpa.BaseDAO;
import ca.gc.aafc.dina.mapper.DinaMapper;
import ca.gc.aafc.dina.repository.DinaRepository;
import ca.gc.aafc.dina.security.RoleBasedPermissionsTest.RoleTestConfig;
import ca.gc.aafc.dina.service.DinaService;
import io.crnk.core.resource.annotations.JsonApiId;
import io.crnk.core.resource.annotations.JsonApiResource;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@SpringBootTest(
  classes = { TestConfiguration.class, RoleTestConfig.class },
  properties = "keycloak.enabled: true")
public class RoleBasedPermissionsTest {

  @Configuration
  @EntityScan(basePackageClasses = RoleBasedPermissionsTest.class)
  static class RoleTestConfig {

    @Bean
    public DinaRepository<Dto, TestEntity> repo(
      @NonNull BaseDAO baseDAO,
      @NonNull DinaFilterResolver filterResolver
    ) {
      return new Repo(baseDAO, filterResolver);
    }
  }

  @Inject
  private DinaRepository<Dto, TestEntity> dinaRepository;

  @BeforeEach
  public void beforeEach() {
    KeycloakAuthenticationToken mockToken = Mockito.mock(
      KeycloakAuthenticationToken.class,
      Answers.RETURNS_DEEP_STUBS);
    TestConfiguration.mockToken(Arrays.asList("/Group1/COLLECTION_MANAGER"), mockToken);
    SecurityContextHolder.getContext().setAuthentication(mockToken);
  }

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

    public Repo(@NonNull BaseDAO baseDAO, @NonNull DinaFilterResolver filterResolver) {
      super(
        new Service(baseDAO),
        Optional.empty(),
        new DinaMapper<>(Dto.class), 
        Dto.class,
        TestEntity.class,
        filterResolver);
    }

    @Override
    @PreAuthorize("hasDinaRole(@currentUser, T(ca.gc.aafc.dina.security.DinaRole).COLLECTION_MANAGER)")
    public <S extends Dto> S create(S resource) {
      return super.create(resource);
    }

  }

  static class Service extends DinaService<TestEntity> {

    public Service(@NonNull BaseDAO baseDAO) {
      super(baseDAO);
    }

  }

}
