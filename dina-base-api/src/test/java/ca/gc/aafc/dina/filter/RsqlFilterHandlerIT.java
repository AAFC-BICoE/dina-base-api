package ca.gc.aafc.dina.filter;

import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.dto.EmployeeDto;
import ca.gc.aafc.dina.dto.PersonDTO;
import ca.gc.aafc.dina.entity.Employee;
import ca.gc.aafc.dina.entity.Person;
import ca.gc.aafc.dina.testsupport.PostgresTestContainerInitializer;
import com.google.common.collect.Sets;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Transactional
@SpringBootTest(classes = TestDinaBaseApp.class, properties = "crnk.allow-unknown-attributes=true")
@ContextConfiguration(initializers = { PostgresTestContainerInitializer.class })
public class RsqlFilterHandlerIT {

  @Inject
  private ResourceRepository<EmployeeDto, Serializable> employeeRepository;
  
  @Inject
  private ResourceRepository<PersonDTO, Serializable> personRepository;
  
  @Inject
  private QuerySpecUrlMapper querySpecUrlMapper;

  @Inject
  private EntityManager entityManager;

  @Inject
  protected ResourceRegistry resourceRegistry;

  private static final OffsetDateTime CREATION_DATE_TIME = OffsetDateTime.parse("2013-07-01T17:55:13-07:00");
  
  @BeforeEach
  public void initEmployees() {
    // Persist 5 test employees.
    entityManager.persist(Employee.builder().uuid(UUID.randomUUID()).name("employee1").build());
    entityManager.persist(Employee.builder().uuid(UUID.randomUUID()).name("employee2").build());
    entityManager.persist(Employee.builder().uuid(UUID.randomUUID()).name("employee3").build());
    entityManager.persist(Employee.builder().uuid(UUID.randomUUID()).name("employee4").build());
    entityManager.persist(Employee.builder().uuid(UUID.randomUUID()).name("employee5").build());
    
    // Persist 5 test People:
    Person person1 = Person.builder().uuid(UUID.randomUUID()).name("person1").createdOn(CREATION_DATE_TIME).build();
    entityManager.persist(person1);
    entityManager.persist(Person.builder().uuid(UUID.randomUUID()).name("person2").createdOn(CREATION_DATE_TIME).build());
    entityManager.persist(Person.builder().uuid(UUID.randomUUID()).name("person3").createdOn(CREATION_DATE_TIME).build());
    entityManager.persist(Person.builder().uuid(UUID.randomUUID()).name("person4").createdOn(CREATION_DATE_TIME).build());
    entityManager.persist(Person.builder().uuid(UUID.randomUUID()).name("person5").createdOn(CREATION_DATE_TIME).build());
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

  @Test
  public void findPersons_whenRsqlFilterIsForUUID_filteredPersonsAreReturned() {
    // Get a UUID from the list of people:
    UUID personUuid = this.personRepository.findAll(new QuerySpec(PersonDTO.class)).get(0).getUuid();
    assertNotNull(personUuid);

    // Filter by uuid:
    QuerySpec querySpec = new QuerySpec(PersonDTO.class);
    querySpec.setFilters(
        Collections.singletonList(
            new FilterSpec(
                Collections.singletonList("rsql"),
                FilterOperator.EQ,
                "uuid==" + personUuid
            )
        )
    );

    // The results should be filtered to the one person with that UUID:
    ResourceList<PersonDTO> persons = this.personRepository.findAll(querySpec);
    assertEquals(1, persons.size());
    assertEquals(personUuid, persons.get(0).getUuid());
  }

  @Test
  public void findPersons_whenRsqlFilterIsForOffsetDateTime_filteredPersonsAreReturned() {
    // Filter by uuid:
    QuerySpec querySpec = new QuerySpec(PersonDTO.class);
    querySpec.setFilters(
        Collections.singletonList(
            new FilterSpec(
                Collections.singletonList("rsql"),
                FilterOperator.EQ,
                "createdOn==" + CREATION_DATE_TIME
            )
        )
    );

    ResourceList<PersonDTO> persons = this.personRepository.findAll(querySpec);
    // All 5 people have the same createdOn time:
    assertEquals(5, persons.size());
    assertEquals(CREATION_DATE_TIME, persons.get(0).getCreatedOn());
  }

  @Test
  void checkFilter_WhenUsingCustomRsqlFilter_FilterApplied() {
    UUID personUuid = this.personRepository.findAll(new QuerySpec(PersonDTO.class)).get(0).getUuid();

    // Filter by custom set filter:
    QuerySpec querySpec = new QuerySpec(PersonDTO.class);
    querySpec.setFilters(Collections.singletonList(
      new FilterSpec(
        Collections.singletonList("rsql"),
        FilterOperator.EQ,
        "customSearch==" + personUuid.toString())));

    // The results should be filtered to the one person with that UUID and not the createdOn filter:
    ResourceList<PersonDTO> persons = this.personRepository.findAll(querySpec);
    assertEquals(1, persons.size());
    assertEquals(personUuid, persons.get(0).getUuid());
  }
}
