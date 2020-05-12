package ca.gc.aafc.dina.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import com.google.common.collect.Sets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import ca.gc.aafc.dina.TestConfiguration;
import ca.gc.aafc.dina.dto.EmployeeDto;
import ca.gc.aafc.dina.entity.Employee;
import io.crnk.core.engine.information.resource.ResourceInformation;
import io.crnk.core.engine.query.QueryContext;
import io.crnk.core.engine.registry.ResourceRegistry;
import io.crnk.core.queryspec.FilterOperator;
import io.crnk.core.queryspec.FilterSpec;
import io.crnk.core.queryspec.PathSpec;
import io.crnk.core.queryspec.QuerySpec;
import io.crnk.core.queryspec.mapper.QuerySpecUrlMapper;
import io.crnk.core.repository.ResourceRepository;
import io.crnk.core.resource.list.ResourceList;

@Transactional
@SpringBootTest(classes = TestConfiguration.class, properties = "crnk.allow-unknown-attributes=true")
public class RsqlFilterHandlerIT {

  @Inject
  private ResourceRepository<EmployeeDto, Serializable> employeeRepository;
  
  @Inject
  private QuerySpecUrlMapper querySpecUrlMapper;

  @Inject
  private EntityManager entityManager;

  @Inject
  protected ResourceRegistry resourceRegistry;
  
  @BeforeEach
  public void initEmployees() {
    // Persist 5 test employees.
    entityManager.persist(Employee.builder().name("employee1").build());
    entityManager.persist(Employee.builder().name("employee2").build());
    entityManager.persist(Employee.builder().name("employee3").build());
    entityManager.persist(Employee.builder().name("employee4").build());
    entityManager.persist(Employee.builder().name("employee5").build());
  }
  
  @Test
  public void findAllEmployees_whenRsqlFilterIsSet_filteredEmployeesAreReturned() {
    // Filter by name = "employee2" or name = "employee4".
    QuerySpec querySpec = new QuerySpec(EmployeeDto.class);
    querySpec.setFilters(
        Collections.singletonList(
            new FilterSpec(
                Collections.singletonList("rsql"),
                FilterOperator.EQ,
                "name==employee2 or name==employee4" // RSQL string
            )
        )
    );
    
    // Check that the 2 employees were returned.
    ResourceList<EmployeeDto> employees = this.employeeRepository.findAll(querySpec);
    assertEquals(2, employees.size());
    assertEquals("employee2", employees.get(0).getName());
    assertEquals("employee4", employees.get(1).getName());
  }
  
  @Test
  public void findAllEmployees_whenRqslFilterIsBlank_allEmployeesAreReturned() {
    // Filter by name = "".
    QuerySpec querySpec = new QuerySpec(EmployeeDto.class);
    querySpec.setFilters(
        Collections.singletonList(
            new FilterSpec(
                Collections.singletonList("rsql"),
                FilterOperator.EQ,
                "" // Blank RSQL string
            )
        )
    );
    
    ResourceList<EmployeeDto> employees = this.employeeRepository.findAll(querySpec);
    assertEquals(5, employees.size());
  }
  
  @Test
  public void findAllEmployees_whenRsqlFilterHasCommas_filteredEmployeesAreReturned() {
    // Filter by name = "employee2" or name = "employee4".
    QuerySpec querySpec = new QuerySpec(EmployeeDto.class);
    querySpec.setFilters(
        Collections.singletonList(
            new FilterSpec(
                Collections.singletonList("rsql"),
                FilterOperator.EQ,
                "name==employee2,name==employee4" // RSQL string
            )
        )
    );
    
    // Check that the 2 employees were returned.
    ResourceList<EmployeeDto> employees = this.employeeRepository.findAll(querySpec);
    assertEquals(2, employees.size());
    assertEquals("employee2", employees.get(0).getName());
    assertEquals("employee4", employees.get(1).getName());
  }
  
  /**
   * For RSQL strings containing commas to work, the Crnk QuerySpecUrlMapper needs to be
   * configured to not convert those strings to HashSets. This test ensures that the
   * QuerySpecUrlMapper is configured correctly.
   */
  @Test
  public void deserializeFilterParam_whenParamHasComma_deserializeFilterAsString() {
    ResourceInformation empInfo = resourceRegistry.findEntry(EmployeeDto.class).getResourceInformation();
    
    Map<String, Set<String>> paramMap = new HashMap<>();
    paramMap.put("filter[rsql]", Sets.newHashSet("name==asd,asd,asd,asd"));
    
    QuerySpec querySpec = querySpecUrlMapper.deserialize(empInfo, paramMap, new QueryContext());
    
    assertEquals(
        "name==asd,asd,asd,asd",
        querySpec.findFilter(PathSpec.of("rsql")).get().getValue()
    );
  }
}
