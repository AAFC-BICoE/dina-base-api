package ca.gc.aafc.dina;

import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

import javax.inject.Inject;

import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.springframework.validation.SmartValidator;

import ca.gc.aafc.dina.dto.DepartmentDto;
import ca.gc.aafc.dina.dto.EmployeeDto;
import ca.gc.aafc.dina.dto.PersonDTO;
import ca.gc.aafc.dina.dto.VocabularyDto;
import ca.gc.aafc.dina.entity.Department;
import ca.gc.aafc.dina.entity.Employee;
import ca.gc.aafc.dina.entity.Person;
import ca.gc.aafc.dina.entity.Vocabulary;
import ca.gc.aafc.dina.filter.DinaFilterResolver;
import ca.gc.aafc.dina.jpa.BaseDAO;
import ca.gc.aafc.dina.jsonapi.DinaRepoEagerLoadingIT;
import ca.gc.aafc.dina.mapper.DinaMapper;
import ca.gc.aafc.dina.repository.DinaRepository;
import ca.gc.aafc.dina.repository.ReadOnlyDinaRepository;
import ca.gc.aafc.dina.security.AllowAllAuthorizationService;
import ca.gc.aafc.dina.security.GroupAuthorizationService;
import ca.gc.aafc.dina.service.AuditService;
import ca.gc.aafc.dina.service.DefaultDinaService;
import ca.gc.aafc.dina.service.DefaultDinaServiceTest.DinaServiceTestImplementation;
import lombok.NonNull;

/**
 * Small test application running on dina-base-api
 */
@SpringBootApplication
@EntityScan(basePackageClasses = Department.class)
@Import(DinaRepoEagerLoadingIT.TestConfig.class)
public class TestDinaBaseApp {

  @Inject
  private GroupAuthorizationService groupAuthService;

  @Bean
  public DinaRepository<DepartmentDto, Department> departmentRepository(BaseDAO baseDAO, DepartmentDinaService departmentDinaService) {
    return new DinaRepository<>(
      departmentDinaService,
      new AllowAllAuthorizationService(),
      Optional.empty(),
      new DinaMapper<>(DepartmentDto.class),
      DepartmentDto.class,
      Department.class,
      null,
      null,
      buildProperties());
  }

  @Bean
  public DinaRepository<EmployeeDto, Employee> employeeRepository(BaseDAO baseDAO, EmployeeDinaService employeeDinaService) {
    return new DinaRepository<>(
      employeeDinaService,
      new AllowAllAuthorizationService(),
      Optional.empty(),
      new DinaMapper<>(EmployeeDto.class),
      EmployeeDto.class,
      Employee.class,
      null,
      null,
      buildProperties());
  }

  @Bean
  public DinaServiceTestImplementation serviceUnderTest(BaseDAO baseDAO, SmartValidator sv) {
    return new DinaServiceTestImplementation(baseDAO, sv);
  }

  @Bean
  public DinaRepository<PersonDTO, Person> dinaRepository(
    DinaPersonService service,
    Optional<AuditService> auditService
  ) {
    DinaMapper<PersonDTO, Person> dinaMapper = new DinaMapper<>(PersonDTO.class);
    return new DinaRepository<>(
      service,
      groupAuthService,
      auditService,
      dinaMapper,
      PersonDTO.class,
      Person.class,
      new DinaFilterResolver(new PersonRsqlAdapter()),
      null,
      buildProperties());
  }

  @Bean
  public ReadOnlyDinaRepository<VocabularyDto, Vocabulary> readOnlyDinaRepository(BaseDAO baseDAO, VocabularyDinaService vocabularyDinaService) {
    DinaMapper<VocabularyDto, Vocabulary> dinaMapper = new DinaMapper<>(VocabularyDto.class);
    return new ReadOnlyDinaRepository<>(
      vocabularyDinaService,
      dinaMapper,
      VocabularyDto.class,
      Vocabulary.class,
      null,
      buildProperties());
  }

  @Bean
  public BuildProperties buildProperties() {
    Properties props = new Properties();
    props.setProperty("version", "test-api-version");
    return new BuildProperties(props);
  }

  @Service
  public static class DinaPersonService extends DefaultDinaService<Person> {

    public DinaPersonService(@NonNull BaseDAO baseDAO, SmartValidator sv) {
      super(baseDAO, sv);
    }

    @Override
    protected void preCreate(Person entity) {
      entity.setUuid(UUID.randomUUID());
    }
  }

  @Service
  public class DepartmentDinaService extends DefaultDinaService<Department> {

    public DepartmentDinaService(@NonNull BaseDAO baseDAO, SmartValidator sv) {
      super(baseDAO, sv);
    }

    @Override
    protected void preCreate(Department entity) {
      entity.setUuid(UUID.randomUUID());
    }

    @Override
    public void validateBusinessRules(Department entity) {
      validateConstraints(entity,null);
    }
  }

  @Service
  public class EmployeeDinaService extends DefaultDinaService<Employee> {
    public EmployeeDinaService(@NonNull BaseDAO baseDAO, SmartValidator sv) {
      super(baseDAO, sv);
    }

    @Override
    public void validateBusinessRules(Employee entity) {
      validateConstraints(entity,null);
    }
  }

  @Service
  public class VocabularyDinaService extends DefaultDinaService<Vocabulary> {

    public VocabularyDinaService(@NonNull BaseDAO baseDAO, SmartValidator sv) {
      super(baseDAO, sv);
    }

  }

  /**
   * Mocks a given token to return a agent identifier and list of given groups.
   *
   * @param keycloakGroupClaim - groups to return in claim
   * @param mockToken          - token to mock
   */
  public static void mockToken(
    List<String> keycloakGroupClaim,
    KeycloakAuthenticationToken mockToken
  ) {
    // Mock the needed fields on the keycloak token:
    Mockito.when(mockToken.getName()).thenReturn("test-user");
    mockClaim(mockToken, "agent-identifier", "a2cef694-10f1-42ec-b403-e0f8ae9d2ae6");
    mockClaim(mockToken, "groups", keycloakGroupClaim);
  }

  /**
   * Mock a given tokens claims by returning a given value for the given claim key.
   *
   * @param token - token holding claims
   * @param key   - key of claim to mock
   * @param value - return value of the claim
   */
  public static void mockClaim(KeycloakAuthenticationToken token, String key, Object value) {
    Mockito.when(
      token.getAccount()
        .getKeycloakSecurityContext()
        .getToken()
        .getOtherClaims()
        .get(key))
      .thenReturn(value);
  }

}
