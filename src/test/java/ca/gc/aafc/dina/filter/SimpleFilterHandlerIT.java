package ca.gc.aafc.dina.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import ca.gc.aafc.dina.TestConfiguration;
import ca.gc.aafc.dina.dto.EmployeeDto;
import ca.gc.aafc.dina.entity.Employee;
import io.crnk.core.queryspec.FilterOperator;
import io.crnk.core.queryspec.FilterSpec;
import io.crnk.core.queryspec.QuerySpec;
import io.crnk.core.repository.ResourceRepository;

@Transactional
@SpringBootTest(classes = TestConfiguration.class)
public class SimpleFilterHandlerIT  {

  @Inject
  private ResourceRepository<EmployeeDto, Serializable> employeeRepository;

  @Inject
  private EntityManager entityManager;
  
  @Test
  public void searchEmployees_whenNameFilterIsSet_filteredEmployeesAreReturned() {
    
    String expectedEmpName = "e2";
    
    Employee emp1 = Employee.builder().name("e1").build();
    
    Employee emp2 = Employee.builder().name(expectedEmpName).build();
    
    Employee emp3 = Employee.builder().name("e3").build();
    
    Employee emp20 = Employee.builder().name("e20").build();
    
    for (Employee newEmp : Arrays.asList(emp1, emp2, emp3, emp20)) {
      entityManager.persist(newEmp);
    }
    
    QuerySpec querySpec = new QuerySpec(EmployeeDto.class);
    querySpec.addFilter(new FilterSpec(Arrays.asList("name"), FilterOperator.EQ, expectedEmpName));
    List<EmployeeDto> empDtos = this.employeeRepository.findAll(querySpec);
    
    assertEquals(
        Arrays.asList(expectedEmpName),
        empDtos.stream().map(EmployeeDto::getName).collect(Collectors.toList())
    );
    
  }

}
