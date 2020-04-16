package ca.gc.aafc.dina.repository.meta;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import ca.gc.aafc.dina.TestConfiguration;
import ca.gc.aafc.dina.dto.DepartmentDto;
import ca.gc.aafc.dina.dto.EmployeeDto;
import ca.gc.aafc.dina.entity.Department;
import ca.gc.aafc.dina.entity.Employee;
import ca.gc.aafc.dina.repository.JpaRelationshipRepository;
import ca.gc.aafc.dina.repository.JpaResourceRepository;
import io.crnk.core.queryspec.FilterOperator;
import io.crnk.core.queryspec.FilterSpec;
import io.crnk.core.queryspec.QuerySpec;
import io.crnk.core.resource.list.ResourceList;
import io.crnk.core.resource.meta.DefaultPagedMetaInformation;

@Transactional
@SpringBootTest(classes = TestConfiguration.class)
public class JpaTotalMetaInformationProviderIT {

  @Inject
  private JpaResourceRepository<EmployeeDto> employeeRepository;
  
  @Inject
  private JpaRelationshipRepository<DepartmentDto, EmployeeDto> departmentToEmployeeRepository;

  @Inject
  private EntityManager entityManager;
  
  private Department testDepartment;

  private static final int NUMBER_OF_EMPLOYEES = 11;

  /**
   * Persist example data for these tests.
   */
  @BeforeEach
  public void persistTestEmployees() {
    testDepartment = Department.builder()
      .name("test dep")
      .location("Ottawa")
      .build();
    entityManager.persist(testDepartment);

    for (int i = 1; i <= NUMBER_OF_EMPLOYEES; i++) {
      Employee employee = Employee.builder()
        .name("test employee " + i)
        .job("job")
        .department(testDepartment)
        .build();
      entityManager.persist(employee);
    }
  }
  
  @Test
  public void jpaResourceRepositoryFindAll_noAdditionalOptions_fullTotalIsIncluded() {
    ResourceList<EmployeeDto> employees = employeeRepository.findAll(new QuerySpec(EmployeeDto.class));
    DefaultPagedMetaInformation meta = (DefaultPagedMetaInformation) employees.getMeta();
    assertEquals(NUMBER_OF_EMPLOYEES, meta.getTotalResourceCount().longValue());
  }
  
  @Test
  public void jpaResourceRepositoryFindAll_whenFilterIsAdded_reducedTotalIsIncluded() {
    QuerySpec querySpec = new QuerySpec(EmployeeDto.class);
    querySpec.addFilter(new FilterSpec(Arrays.asList("name"), FilterOperator.EQ, "test employee 5"));
    
    ResourceList<EmployeeDto> employees = employeeRepository.findAll(querySpec);
    DefaultPagedMetaInformation meta = (DefaultPagedMetaInformation) employees.getMeta();
    assertEquals(1, employees.size());
    assertEquals(1, meta.getTotalResourceCount().longValue());
  }
  
  @Test
  public void jpaRelationshipRepository_whenFilterIsAdded_reducedTotalIsIncluded() {
    QuerySpec querySpec = new QuerySpec(EmployeeDto.class);
    querySpec.addFilter(new FilterSpec(Arrays.asList("name"), FilterOperator.EQ, "test employee 3"));
    ResourceList<EmployeeDto> employees = departmentToEmployeeRepository
        .findManyTargets(testDepartment.getUuid(), "employees", querySpec);
    DefaultPagedMetaInformation meta = (DefaultPagedMetaInformation) employees.getMeta();
    
    assertEquals(1, employees.size());
    assertEquals(1, meta.getTotalResourceCount().longValue());
  }
  
  @Test
  public void jpaResourceRepositoryFindAll_whenPageLimitIsSpecified_fullTotalIsIncluded() {
    QuerySpec querySpec = new QuerySpec(EmployeeDto.class);
    querySpec.setLimit(1L);

    ResourceList<EmployeeDto> employees = employeeRepository.findAll(querySpec);
    DefaultPagedMetaInformation meta = (DefaultPagedMetaInformation) employees.getMeta();
    assertEquals(1, employees.size());
    assertEquals(NUMBER_OF_EMPLOYEES, meta.getTotalResourceCount().longValue());
  }
  
}
