package ca.gc.aafc.dina;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;

import ca.gc.aafc.dina.dto.DepartmentDto;
import ca.gc.aafc.dina.dto.EmployeeDto;
import ca.gc.aafc.dina.dto.PersonDTO;
import ca.gc.aafc.dina.entity.ComplexObject;
import ca.gc.aafc.dina.entity.Department;
import ca.gc.aafc.dina.entity.Employee;
import ca.gc.aafc.dina.entity.Person;
import ca.gc.aafc.dina.filter.DinaFilterResolver;
import ca.gc.aafc.dina.filter.RsqlFilterHandler;
import ca.gc.aafc.dina.filter.SimpleFilterHandler;
import ca.gc.aafc.dina.jpa.BaseDAO;
import ca.gc.aafc.dina.mapper.CustomFieldResolverSpec;
import ca.gc.aafc.dina.mapper.DinaMapper;
import ca.gc.aafc.dina.mapper.JpaDtoMapper;
import ca.gc.aafc.dina.repository.DinaRepository;
import ca.gc.aafc.dina.repository.DinaRepositoryIT.DinaPersonService;
import ca.gc.aafc.dina.repository.JpaDtoRepository;
import ca.gc.aafc.dina.repository.JpaRelationshipRepository;
import ca.gc.aafc.dina.repository.JpaResourceRepository;
import ca.gc.aafc.dina.repository.meta.JpaTotalMetaInformationProvider;
import ca.gc.aafc.dina.service.DinaServiceTest.DinaServiceTestImplementation;

/**
 * Small test application running on dina-base-api
 */
@SpringBootApplication
@EntityScan(basePackageClasses = Department.class)
public class TestConfiguration {

  @Inject
  private JpaTotalMetaInformationProvider metaInformationProvider;

  @Inject
  private SimpleFilterHandler simpleFilterHandler;

  @Inject
  private RsqlFilterHandler rsqlFilterHandler;

  @Inject
  private DinaFilterResolver filterResolver;

  @Bean
  public JpaDtoMapper jpaDtoMapper() {
    Map<Class<?>, Class<?>> jpaEntities = new HashMap<>();

    jpaEntities.put(DepartmentDto.class, Department.class);
    jpaEntities.put(EmployeeDto.class, Employee.class);

    Map<Class<?>, List<CustomFieldResolverSpec<?>>> customFieldResolvers = new HashMap<>();

    // EmployeeDto custom resolvers go here:
    customFieldResolvers.put(EmployeeDto.class, Arrays.asList(
      CustomFieldResolverSpec.<Employee>builder()
        .field("nameUppercase")
        .resolver(employee -> StringUtils.upperCase(employee.getName()))
        .build(),
      CustomFieldResolverSpec.<Employee>builder()
        .field("customField")
        .resolver(employee ->  
            employee.getCustomField() == null ? "" : employee.getCustomField().getName())
        .build()
    ));

    customFieldResolvers.put(Employee.class, Arrays.asList(
      CustomFieldResolverSpec.<EmployeeDto>builder()
        .field("customField")
        .resolver(employeeDto -> ComplexObject.builder().name(employeeDto.getCustomField()).build())
        .build()));

    customFieldResolvers.put(DepartmentDto.class, Arrays.asList(
      CustomFieldResolverSpec.<Department>builder()
        .field("employeeCount")
        .resolver(dept -> dept.getEmployees().size())
        .build()
    ));

    return new JpaDtoMapper(jpaEntities, customFieldResolvers);
  }

  @Bean
  public JpaResourceRepository<DepartmentDto> departmentRepository(JpaDtoRepository dtoRepository) {
    return new JpaResourceRepository<DepartmentDto>(
      DepartmentDto.class,
      dtoRepository,
      Arrays.asList(simpleFilterHandler, rsqlFilterHandler),
      metaInformationProvider
    );
  }

  @Bean
  public JpaResourceRepository<EmployeeDto> employeeRepository(JpaDtoRepository dtoRepository) {
    return new JpaResourceRepository<EmployeeDto>(
      EmployeeDto.class,
      dtoRepository,
      Arrays.asList(simpleFilterHandler, rsqlFilterHandler),
      metaInformationProvider
    );
  }

  @Bean
  public JpaRelationshipRepository<DepartmentDto, EmployeeDto> departmentToEmployeeRepository(JpaDtoRepository dtoRepository) {
    return new JpaRelationshipRepository<>(
      DepartmentDto.class,
      EmployeeDto.class,
      dtoRepository,
      Arrays.asList(
        simpleFilterHandler, 
        rsqlFilterHandler
      ),
      metaInformationProvider
    );
  }

  @Bean
  public JpaRelationshipRepository<EmployeeDto, DepartmentDto> employeeToDepartmentRepository(JpaDtoRepository dtoRepository) {
    return new JpaRelationshipRepository<>(
      EmployeeDto.class,
      DepartmentDto.class,
      dtoRepository,
      Arrays.asList(
        simpleFilterHandler, 
        rsqlFilterHandler
      ),
      metaInformationProvider
    );
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
  public DinaRepository<PersonDTO, Person> dinaRepository(DinaPersonService service) {
    DinaMapper<PersonDTO, Person> dinaMapper = new DinaMapper<>(PersonDTO.class);
    return new DinaRepository<>(service, dinaMapper, PersonDTO.class, Person.class, filterResolver);
  }

}
