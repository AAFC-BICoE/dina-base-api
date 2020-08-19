package ca.gc.aafc.dina.security;

import ca.gc.aafc.dina.DinaBaseApiAutoConfiguration;
import ca.gc.aafc.dina.TestConfiguration;
import ca.gc.aafc.dina.dto.DepartmentDto;
import ca.gc.aafc.dina.dto.EmployeeDto;
import ca.gc.aafc.dina.dto.PersonDTO;
import ca.gc.aafc.dina.entity.Department;
import ca.gc.aafc.dina.entity.Employee;
import ca.gc.aafc.dina.entity.Person;
import ca.gc.aafc.dina.filter.DinaFilterResolver;
import ca.gc.aafc.dina.jpa.BaseDAO;
import ca.gc.aafc.dina.mapper.DinaMapper;
import ca.gc.aafc.dina.mapper.JpaDtoMapper;
import ca.gc.aafc.dina.repository.DinaRepository;
import ca.gc.aafc.dina.repository.DinaRepositoryIT.DinaPersonService;
import ca.gc.aafc.dina.security.spring.RoleAuthenticationProxy;
import ca.gc.aafc.dina.service.DinaService;
import ca.gc.aafc.dina.service.RoleAuthorizationService;
import com.google.common.collect.ImmutableSet;
import io.crnk.core.repository.ResourceRepository;
import lombok.NonNull;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.mockito.Answers;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(properties = "keycloak.enabled = true")
@ActiveProfiles({"RoleBasedPermissionsTest"})
@Transactional
public class RoleBasedPermissionsIT {

  @Inject
  private DinaRepository<PersonDTO, Person> collectionRepo;
  @Inject
  private DinaRepository<DepartmentDto, Department> staffRepo;
  @Inject
  private DepService depService;

  @BeforeEach
  void setUp() {
    mockRole(DinaRole.COLLECTION_MANAGER);
  }

  @Test
  public void create_AuthorizedUser_AllowsOperation() {
    PersonDTO dto = collectionRepo.create(new PersonDTO());
    assertNotNull(dto.getUuid());
  }

  @Test
  public void create_UnAuthorizedUser_ThrowsForbiddenException() {
    assertThrows(AccessDeniedException.class, () -> staffRepo.create(new DepartmentDto()));
  }

  @Test
  public void update_AuthorizedUser_AllowsOperation() {
    PersonDTO dto = collectionRepo.create(new PersonDTO());
    dto.setName(RandomStringUtils.random(4));
    collectionRepo.save(dto);
  }

  @Test
  public void update_UnAuthorizedUser_ThrowsForbiddenException() {
    UUID id = depService.create(new Department()).getUuid();
    assertThrows(
      AccessDeniedException.class,
      () -> staffRepo.save(DepartmentDto.builder().uuid(id).build()));
  }

  @Test
  public void delete_AuthorizedUser_AllowsOperation() {
    PersonDTO dto = collectionRepo.create(new PersonDTO());
    collectionRepo.delete(dto.getUuid());
  }

  @Test
  public void delete_UnAuthorizedUser_ThrowsForbiddenException() {
    UUID id = depService.create(new Department()).getUuid();
    assertThrows(AccessDeniedException.class, () -> staffRepo.delete(id));
  }

  @Configuration
  @ComponentScan(basePackageClasses = DinaBaseApiAutoConfiguration.class, excludeFilters = {
    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = TestConfiguration.class)})
  @Profile({"RoleBasedPermissionsTest"})
  @EntityScan(basePackageClasses = {RoleBasedPermissionsIT.class, Department.class})
  static class RoleTestConfig {

    @Bean
    public JpaDtoMapper jpaDtoMapper() {
      return new JpaDtoMapper(new HashMap<>(), new HashMap<>());
    }

    @Bean
    public DepService deptService(BaseDAO baseDAO) {
      return new DepService(baseDAO);
    }

    @Bean
    public DinaRepository<DepartmentDto, Department> staffBasedRepo(
      DinaFilterResolver filterResolver,
      RoleAuthenticationProxy proxy,
      DepService dinaService
    ) {
      return new StaffBasedRepo(filterResolver, proxy, dinaService);
    }

    @Bean
    public DinaRepository<PersonDTO, Person> collectionBasedRepo(
      BaseDAO baseDAO,
      DinaFilterResolver filterResolver,
      RoleAuthenticationProxy proxy
    ) {
      return new CollectionBasedRepo(baseDAO, filterResolver, proxy);
    }

    @Bean
    @SuppressWarnings("unchecked")
    public ResourceRepository<EmployeeDto, Employee> mockEmployeeRepo() {
      ResourceRepository<EmployeeDto, Employee> mock = Mockito.mock(ResourceRepository.class);
      BDDMockito.given(mock.getResourceClass()).willReturn(EmployeeDto.class);
      return mock;
    }
  }

  static class CollectionBasedRepo extends DinaRepository<PersonDTO, Person> {

    public CollectionBasedRepo(
      BaseDAO baseDAO,
      DinaFilterResolver filterResolver,
      RoleAuthenticationProxy proxy
    ) {
      super(
        new DinaPersonService(baseDAO),
        Optional.of(new RoleAuthorizationService(
          proxy,
          ImmutableSet.of(DinaRole.COLLECTION_MANAGER))),
        Optional.empty(),
        new DinaMapper<>(PersonDTO.class),
        PersonDTO.class,
        Person.class,
        filterResolver);
    }
  }

  static class StaffBasedRepo extends DinaRepository<DepartmentDto, Department> {

    public StaffBasedRepo(
      DinaFilterResolver filterResolver,
      RoleAuthenticationProxy proxy, DepService dinaService
    ) {
      super(
        dinaService,
        Optional.of(new RoleAuthorizationService(
          proxy,
          ImmutableSet.of(DinaRole.STAFF))),
        Optional.empty(),
        new DinaMapper<>(DepartmentDto.class),
        DepartmentDto.class,
        Department.class,
        filterResolver);
    }
  }

  static class DepService extends DinaService<Department> {

    public DepService(@NonNull BaseDAO baseDAO) {
      super(baseDAO);
    }

    @Override
    protected void preCreate(Department entity) {
      entity.setUuid(UUID.randomUUID());
    }
  }

  private void mockRole(DinaRole role) {
    KeycloakAuthenticationToken mockToken = Mockito.mock(
      KeycloakAuthenticationToken.class,
      Answers.RETURNS_DEEP_STUBS);
    TestConfiguration.mockToken(Arrays.asList("/GROUP_1/" + role.name()), mockToken);
    SecurityContextHolder.getContext().setAuthentication(mockToken);
  }

}
