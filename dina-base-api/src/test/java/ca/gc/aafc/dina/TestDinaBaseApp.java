package ca.gc.aafc.dina;

import ca.gc.aafc.dina.DinaUserConfig.DepartmentDinaService;
import ca.gc.aafc.dina.DinaUserConfig.EmployeeDinaService;
import ca.gc.aafc.dina.DinaUserConfig.VocabularyDinaService;
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
import ca.gc.aafc.dina.mapper.DinaMapper;
import ca.gc.aafc.dina.repository.DinaRepository;
import ca.gc.aafc.dina.repository.DinaRepositoryIT.DinaPersonService;
import ca.gc.aafc.dina.repository.ReadOnlyDinaRepository;
import ca.gc.aafc.dina.service.AuditService;
import ca.gc.aafc.dina.service.DefaultDinaService;
import ca.gc.aafc.dina.service.DefaultDinaServiceTest.DinaServiceTestImplementation;
import ca.gc.aafc.dina.service.GroupAuthorizationService;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

/**
 * Small test application running on dina-base-api
 */
@SpringBootApplication
@EntityScan(basePackageClasses = Department.class)
public class TestDinaBaseApp {

  @Inject
  private Optional<GroupAuthorizationService> groupAuthService;

  @Bean
  public DinaRepository<DepartmentDto, Department> departmentRepository(BaseDAO baseDAO, DepartmentDinaService departmentDinaService) {
    return new DinaRepository<>(
      departmentDinaService,
      Optional.empty(),
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
      Optional.empty(),
      Optional.empty(),
      new DinaMapper<>(EmployeeDto.class),
      EmployeeDto.class,
      Employee.class,
      null, 
      null,
      buildProperties());
  }

  @Bean
  public DinaServiceTestImplementation serviceUnderTest(BaseDAO baseDAO) {
    return new DinaServiceTestImplementation(baseDAO);
  }

  @Bean
  public DinaPersonService personService(BaseDAO baseDAO) {
    return new DinaPersonService(baseDAO);
  }

  @Bean
  public DinaRepository<PersonDTO, Person> dinaRepository(
    DinaPersonService service,
    Optional<AuditService> auditService
  ) {
    DinaMapper<PersonDTO, Person> dinaMapper = new DinaMapper<>(PersonDTO.class);
    return new DinaRepository<>(
      service,
      Optional.ofNullable(groupAuthService.orElse(null)),
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
