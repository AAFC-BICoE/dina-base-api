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
import ca.gc.aafc.dina.service.DinaAuthorizationService;
import ca.gc.aafc.dina.service.RoleAuthorizationService;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.crnk.core.exception.ForbiddenException;
import io.crnk.core.repository.ResourceRepository;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.*;
import org.springframework.test.context.ActiveProfiles;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles({ "RoleBasedPermissionsTest" })
public class RoleBasedPermissionsIT {

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

    @Bean(name = "roleBasedUser")
    public DinaAuthenticatedUser user() {
      return Mockito.mock(DinaAuthenticatedUser.class);
    }

    @Bean
    @SuppressWarnings("unchecked")
    public DinaRepository<DepartmentDto, Department> mockDepartmentRepo() {
      DinaRepository<DepartmentDto, Department> mock = Mockito.mock(DinaRepository.class);
      BDDMockito.given(mock.getResourceClass()).willReturn(DepartmentDto.class);
      return mock;
    }

    @Bean
    @SuppressWarnings("unchecked")
    public ResourceRepository<EmployeeDto, Employee> mockEmployeeRepo() {
      ResourceRepository<EmployeeDto, Employee> mock = Mockito.mock(ResourceRepository.class);
      BDDMockito.given(mock.getResourceClass()).willReturn(EmployeeDto.class);
      return mock;
    }

    @Bean
    public DinaRepository<PersonDTO, Person> repo(
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
  private DinaRepository<PersonDTO, Person> dinaRepository;

  @Inject
  public DinaAuthenticatedUser roleBasedUser;

  private static final Map<String, Set<DinaRole>> ROLES_PER_GROUP = ImmutableMap.of("group 1",
      ImmutableSet.of(DinaRole.COLLECTION_MANAGER));
  private static final Map<String, Set<DinaRole>> INVALID_ROLES = ImmutableMap.of("group 1",
      ImmutableSet.of(DinaRole.STAFF));

  @Test
  public void create_AuthorizedUser_AllowsOperation() {
    BDDMockito.given(this.roleBasedUser.getRolesPerGroup()).willReturn(ROLES_PER_GROUP);
    PersonDTO dto = dinaRepository.create(new PersonDTO());
    assertNotNull(dto.getUuid());
  }

  @Test
  public void create_UnAuthorizedUser_ThrowsForbiddenException() {
    BDDMockito.given(this.roleBasedUser.getRolesPerGroup()).willReturn(INVALID_ROLES);
    assertThrows(ForbiddenException.class, () -> dinaRepository.create(new PersonDTO()));
  }

  @Test
  public void update_AuthorizedUser_AllowsOperation() {
    BDDMockito.given(this.roleBasedUser.getRolesPerGroup()).willReturn(ROLES_PER_GROUP);
    PersonDTO dto = dinaRepository.create(new PersonDTO());
    dto.setName(RandomStringUtils.random(4));
    dinaRepository.save(dto);
  }

  @Test
  public void update_UnAuthorizedUser_ThrowsForbiddenException() {
    BDDMockito.given(this.roleBasedUser.getRolesPerGroup()).willReturn(ROLES_PER_GROUP);
    PersonDTO dto = dinaRepository.create(new PersonDTO());
    BDDMockito.given(this.roleBasedUser.getRolesPerGroup()).willReturn(INVALID_ROLES);
    assertThrows(ForbiddenException.class, () -> dinaRepository.save(dto));
  }

  @Test
  public void delete_AuthorizedUser_AllowsOperation() {
    BDDMockito.given(this.roleBasedUser.getRolesPerGroup()).willReturn(ROLES_PER_GROUP);
    PersonDTO dto = dinaRepository.create(new PersonDTO());
    dinaRepository.delete(dto.getUuid());
  }

  @Test
  public void delete_UnAuthorizedUser_ThrowsForbiddenException() {
    BDDMockito.given(this.roleBasedUser.getRolesPerGroup()).willReturn(ROLES_PER_GROUP);
    PersonDTO dto = dinaRepository.create(new PersonDTO());
    BDDMockito.given(this.roleBasedUser.getRolesPerGroup()).willReturn(INVALID_ROLES);
    assertThrows(ForbiddenException.class, () -> dinaRepository.delete(dto.getUuid()));
  }

  static class Repo extends DinaRepository<PersonDTO, Person> {

    public Repo(
      BaseDAO baseDAO,
      Optional<DinaAuthorizationService> authorizationService,
      DinaFilterResolver filterResolver
    ) {
      super(
        new DinaPersonService(baseDAO),
        authorizationService,
        Optional.empty(),
        new DinaMapper<>(PersonDTO.class), 
        PersonDTO.class,
        Person.class,
        filterResolver);
    }

  }

}
