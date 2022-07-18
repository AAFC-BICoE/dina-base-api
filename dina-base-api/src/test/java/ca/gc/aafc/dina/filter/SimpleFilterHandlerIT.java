package ca.gc.aafc.dina.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import ca.gc.aafc.dina.testsupport.PostgresTestContainerInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.dto.EmployeeDto;
import ca.gc.aafc.dina.dto.PersonDTO;
import ca.gc.aafc.dina.entity.Employee;
import ca.gc.aafc.dina.entity.Person;
import io.crnk.core.queryspec.FilterOperator;
import io.crnk.core.queryspec.FilterSpec;
import io.crnk.core.queryspec.QuerySpec;
import io.crnk.core.repository.ResourceRepository;
import org.springframework.test.context.ContextConfiguration;

@Transactional
@SpringBootTest(classes = TestDinaBaseApp.class)
@ContextConfiguration(initializers = { PostgresTestContainerInitializer.class })
public class SimpleFilterHandlerIT  {

  @Inject
  private ResourceRepository<EmployeeDto, Serializable> employeeRepository;

  @Inject
  private ResourceRepository<PersonDTO, Serializable> personRepository;

  @Inject
  private EntityManager entityManager;
  
  @Test
  public void searchEmployees_whenNameFilterIsSet_filteredEmployeesAreReturned() {
    
    String expectedEmpName = "e2";
    
    Employee emp1 = Employee.builder().uuid(UUID.randomUUID()).name("e1").build();
    Employee emp2 = Employee.builder().uuid(UUID.randomUUID()).name(expectedEmpName).build();
    Employee emp3 = Employee.builder().uuid(UUID.randomUUID()).name("e3").build();
    Employee emp20 = Employee.builder().uuid(UUID.randomUUID()).name("e20").build();
    
    for (Employee newEmp : Arrays.asList(emp1, emp2, emp3, emp20)) {
      entityManager.persist(newEmp);
    }
    
    QuerySpec querySpec = new QuerySpec(EmployeeDto.class);
    querySpec.addFilter(new FilterSpec(List.of("name"), FilterOperator.EQ, expectedEmpName));
    List<EmployeeDto> empDtos = this.employeeRepository.findAll(querySpec);
    
    assertEquals(List.of(expectedEmpName),
        empDtos.stream().map(EmployeeDto::getName).collect(Collectors.toList())
    );
    
  }

  @Test
  public void getRestriction_EqualsNull_FiltersOnEqualsNull() {
    Employee hasJob = Employee.builder().uuid(UUID.randomUUID()).name("hasJob").job("has a job").build();
    Employee noJob = Employee.builder().uuid(UUID.randomUUID()).name("noJob").build();
    entityManager.persist(hasJob);
    entityManager.persist(noJob);

    QuerySpec querySpec = new QuerySpec(EmployeeDto.class);
    querySpec.addFilter(new FilterSpec(List.of("job"), FilterOperator.EQ, null));
    List<EmployeeDto> empDtos = this.employeeRepository.findAll(querySpec);

    assertEquals(List.of(noJob.getName()),
      empDtos.stream().map(EmployeeDto::getName).collect(Collectors.toList()));
  }

  @Test
  public void getRestriction_EqualsNotNull_FiltersOnEqualsNotNull() {
    Employee hasJob = Employee.builder().uuid(UUID.randomUUID()).name("hasJob").job("has a job").build();
    Employee noJob = Employee.builder().uuid(UUID.randomUUID()).name("noJob").build();
    entityManager.persist(hasJob);
    entityManager.persist(noJob);

    QuerySpec querySpec = new QuerySpec(EmployeeDto.class);
    querySpec.addFilter(new FilterSpec(List.of("job"), FilterOperator.NEQ, null));
    List<EmployeeDto> empDtos = this.employeeRepository.findAll(querySpec);

    assertEquals(List.of(hasJob.getName()),
      empDtos.stream().map(EmployeeDto::getName).collect(Collectors.toList()));
  }

  @Test
  public void getRestriction_whenFieldIsUuid_filtersByUuid() {
    Person person1 = Person.builder().uuid(UUID.randomUUID()).name("person1").build();
    Person person2 = Person.builder().uuid(UUID.randomUUID()).name("person2").build();
    entityManager.persist(person1);
    entityManager.persist(person2);

    // Filter by uuid:
    QuerySpec querySpec = new QuerySpec(PersonDTO.class);
    querySpec.addFilter(new FilterSpec(List.of("uuid"), FilterOperator.EQ, person1.getUuid().toString()));
    List<PersonDTO> personDtos = this.personRepository.findAll(querySpec);

    assertEquals(List.of(person1.getUuid()),
      personDtos.stream().map(PersonDTO::getUuid).collect(Collectors.toList())
    );
  }

  @Test
  public void getRestriction_whenFieldIsOffsetDateTime_filtersByOffsetDateTime() {
    OffsetDateTime creationDateTime = OffsetDateTime.parse("2013-07-01T17:55:13-07:00");

    Person person1 = Person.builder().uuid(UUID.randomUUID()).name("person1").createdOn(creationDateTime).build();
    Person person2 = Person.builder().uuid(UUID.randomUUID()).name("person2").createdOn(creationDateTime).build();
    entityManager.persist(person1);
    entityManager.persist(person2);
    creationDateTime = person1.getCreatedOn();

    // Filter by uuid:
    QuerySpec querySpec = new QuerySpec(PersonDTO.class);
    querySpec.addFilter(new FilterSpec(List.of("createdOn"), FilterOperator.EQ, creationDateTime.toString()));
    List<PersonDTO> personDtos = this.personRepository.findAll(querySpec);

    assertEquals(
      Arrays.asList(person1.getCreatedOn(), person2.getCreatedOn()),
      personDtos.stream().map(PersonDTO::getCreatedOn).collect(Collectors.toList())
    );
  }

}
