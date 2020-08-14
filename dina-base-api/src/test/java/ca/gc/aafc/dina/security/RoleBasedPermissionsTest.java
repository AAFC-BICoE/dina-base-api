package ca.gc.aafc.dina.security;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import org.apache.commons.lang3.RandomStringUtils;
import org.hibernate.annotations.NaturalId;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ca.gc.aafc.dina.TestConfiguration;
import ca.gc.aafc.dina.dto.RelatedEntity;
import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.filter.DinaFilterResolver;
import ca.gc.aafc.dina.jpa.BaseDAO;
import ca.gc.aafc.dina.mapper.DinaMapper;
import ca.gc.aafc.dina.repository.DinaRepository;
import ca.gc.aafc.dina.security.RoleBasedPermissionsTest.RoleTestConfig;
import ca.gc.aafc.dina.service.DinaAuthorizationService;
import ca.gc.aafc.dina.service.DinaService;
import ca.gc.aafc.dina.service.RoleAuthorizationService;
import io.crnk.core.exception.ForbiddenException;
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

    @Bean(name = "roleBasedUser")
    public DinaAuthenticatedUser user() {
      return Mockito.mock(DinaAuthenticatedUser.class);
    };

    @Bean
    public DinaRepository<Dto, TestEntity> repo(
      BaseDAO baseDAO,
      DinaFilterResolver filterResolver,
      DinaAuthenticatedUser roleBasedUser
    ) {
      return new Repo(
        baseDAO,
        Optional.of(new RoleAuthorizationService(DinaRole.COLLECTION_MANAGER, roleBasedUser)),
        filterResolver);
    }
  }

  @Inject
  private DinaRepository<Dto, TestEntity> dinaRepository;

  @Inject
  public DinaAuthenticatedUser roleBasedUser;

  private static final Map<String, Set<DinaRole>> ROLES_PER_GROUP = ImmutableMap.of("group 1",
      ImmutableSet.of(DinaRole.COLLECTION_MANAGER));
  private static final Map<String, Set<DinaRole>> INVALID_ROLES = ImmutableMap.of("group 1",
      ImmutableSet.of(DinaRole.STAFF));

  @Test
  public void create_AuthorizedUser_AllowsOperation() {
    BDDMockito.given(this.roleBasedUser.getRolesPerGroup()).willReturn(ROLES_PER_GROUP);
    Dto dto = dinaRepository.create(new Dto());
    assertNotNull(dto.getUuid());
  }

  @Test
  public void create_UnAuthorizedUser_ThrowsForbiddenException() {
    BDDMockito.given(this.roleBasedUser.getRolesPerGroup()).willReturn(INVALID_ROLES);
    assertThrows(ForbiddenException.class, () -> dinaRepository.create(new Dto()));
  }

  @Test
  public void update_AuthorizedUser_AllowsOperation() {
    BDDMockito.given(this.roleBasedUser.getRolesPerGroup()).willReturn(ROLES_PER_GROUP);
    Dto dto = dinaRepository.create(new Dto());
    dto.setName(RandomStringUtils.random(4));
    dinaRepository.save(dto);
  }

  @Test
  public void update_UnAuthorizedUser_ThrowsForbiddenException() {
    BDDMockito.given(this.roleBasedUser.getRolesPerGroup()).willReturn(ROLES_PER_GROUP);
    Dto dto = dinaRepository.create(new Dto());
    BDDMockito.given(this.roleBasedUser.getRolesPerGroup()).willReturn(INVALID_ROLES);
    assertThrows(ForbiddenException.class, () -> dinaRepository.save(dto));
  }

  @Test
  public void delete_AuthorizedUser_AllowsOperation() {
    BDDMockito.given(this.roleBasedUser.getRolesPerGroup()).willReturn(ROLES_PER_GROUP);
    Dto dto = dinaRepository.create(new Dto());
    dinaRepository.delete(dto.getUuid());
  }

  @Test
  public void delete_UnAuthorizedUser_ThrowsForbiddenException() {
    BDDMockito.given(this.roleBasedUser.getRolesPerGroup()).willReturn(ROLES_PER_GROUP);
    Dto dto = dinaRepository.create(new Dto());
    BDDMockito.given(this.roleBasedUser.getRolesPerGroup()).willReturn(INVALID_ROLES);
    assertThrows(ForbiddenException.class, () -> dinaRepository.delete(dto.getUuid()));
  }

  @Data
  @NoArgsConstructor
  @Entity
  public static class TestEntity implements DinaEntity {
    @Id
    @GeneratedValue
    private Integer id;
    @NaturalId
    private UUID uuid;
    private String name;
  }

  @Data
  @JsonApiResource(type = "Dto")
  @RelatedEntity(TestEntity.class)
  public static class Dto {
    @JsonApiId
    private UUID uuid;
    private String name;
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

    @Override
    protected void preCreate(TestEntity entity) {
      entity.setUuid(UUID.randomUUID());
    }

  }

}
