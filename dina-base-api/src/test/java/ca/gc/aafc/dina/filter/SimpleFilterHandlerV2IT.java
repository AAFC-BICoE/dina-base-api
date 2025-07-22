package ca.gc.aafc.dina.filter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.querydsl.core.types.Ops;

import ca.gc.aafc.dina.BasePostgresItContext;
import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.dto.PersonDTO;
import ca.gc.aafc.dina.entity.Person;
import ca.gc.aafc.dina.exception.UnknownAttributeException;
import ca.gc.aafc.dina.filter.FilterGroup.Conjunction;
import ca.gc.aafc.dina.repository.DinaRepositoryV2;
import ca.gc.aafc.dina.repository.DinaRepositoryV2IT;
import ca.gc.aafc.dina.testsupport.factories.TestableEntityFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;

@SpringBootTest(classes = {TestDinaBaseApp.class, DinaRepositoryV2IT.RepoV2TestConfig.class})
@Transactional
public class SimpleFilterHandlerV2IT extends BasePostgresItContext {

  @Autowired
  private DinaRepositoryV2<PersonDTO, Person> personRepository;

  @Inject
  private EntityManager entityManager;

  @Test
  public void searchEmployees_whenNameFilterIsEq_filteredEmployeesAreReturned() {
    String expectedEmpName = "e2";
    
    Person emp1 = Person.builder().uuid(UUID.randomUUID()).name("e1").build();
    Person emp2 = Person.builder().uuid(UUID.randomUUID()).name(expectedEmpName).build();
    Person emp3 = Person.builder().uuid(UUID.randomUUID()).name("e3").build();
    Person emp20 = Person.builder().uuid(UUID.randomUUID()).name("e20").build();
    
    for (Person newPerson : Arrays.asList(emp1, emp2, emp3, emp20)) {
      entityManager.persist(newPerson);
    }

    QueryComponent qc = QueryComponent.builder()
      .filters(new FilterExpression("name", Ops.EQ, expectedEmpName))
      .build();

    var personDtos = this.personRepository.getAll(qc);
    assertEquals(List.of(expectedEmpName),
      personDtos.resourceList().stream().map( m -> m.getDto().getName()).collect(Collectors.toList())
    );
  }

  @Test
  public void searchEmployees_whenNameFilterIsEqList_filteredEmployeesAreReturned() {
    Person emp1 = Person.builder().uuid(UUID.randomUUID()).name("e1").build();
    Person emp2 = Person.builder().uuid(UUID.randomUUID()).name("e2").build();
    Person emp3 = Person.builder().uuid(UUID.randomUUID()).name("e3").build();

    for (Person newPerson : Arrays.asList(emp1, emp2, emp3)) {
      entityManager.persist(newPerson);
    }

    QueryComponent qc = QueryComponent.builder()
        .filters(FilterGroup.builder()
            .conjunction(Conjunction.OR)
            .component(new FilterExpression("name", Ops.EQ, "e1"))
            .component(new FilterExpression("name", Ops.EQ, "e2"))
            .build())
        .build();

    var personDtos = this.personRepository.getAll(qc);
    assertEquals(2, personDtos.resourceList().size());
    assertEquals(List.of("e1", "e2"),
      personDtos.resourceList().stream().map( m -> m.getDto().getName()).collect(Collectors.toList())
    );
  }

  @Test
  public void searchEmployees_whenNameFilterIsLike_filteredEmployeesAreReturned() {
    String expectedEmpName = "e2abc";

    Person emp1 = Person.builder().uuid(UUID.randomUUID()).name("e1").build();
    Person emp2 = Person.builder().uuid(UUID.randomUUID()).name(expectedEmpName).build();
    Person emp3 = Person.builder().uuid(UUID.randomUUID()).name("e3").build();
    Person emp20 = Person.builder().uuid(UUID.randomUUID()).name("e20").build();

    for (Person newPerson : Arrays.asList(emp1, emp2, emp3, emp20)) {
      entityManager.persist(newPerson);
    }

    QueryComponent qc = QueryComponent.builder()
      .filters(new FilterExpression("name", Ops.LIKE,
        expectedEmpName.replace("c", "%")))
      .build();

    var personDtos = this.personRepository.getAll(qc);
    assertEquals(List.of(expectedEmpName),
      personDtos.resourceList().stream().map( m -> m.getDto().getName()).collect(Collectors.toList())
    );
  }

  @Test
  public void searchEmployees_whenRSQL_exception() {
    String rsqlQuery = "filter[rsql]=name==c";
    assertThrows(UnknownAttributeException.class, () -> this.personRepository.getAll(rsqlQuery));
  }

  @Test
  public void searchEmployees_unknownSort_exception() {
    String rsqlQuery = "sort=abc";
    assertThrows(UnknownAttributeException.class, () -> this.personRepository.getAll(rsqlQuery));
  }

  /**
   * Case-insensitive like
   */
  @Test
  public void searchEmployees_whenNameFilterIsILike_filteredEmployeesAreReturned() {
    String expectedEmpName = "E2ABC";

    Person emp1 = Person.builder().uuid(UUID.randomUUID()).name("E1").build();
    Person emp2 = Person.builder().uuid(UUID.randomUUID()).name(expectedEmpName).build();
    Person emp3 = Person.builder().uuid(UUID.randomUUID()).name("E3").build();
    Person emp20 = Person.builder().uuid(UUID.randomUUID()).name("E20").build();

    for (Person newPerson : Arrays.asList(emp1, emp2, emp3, emp20)) {
      entityManager.persist(newPerson);
    }

    QueryComponent qc = QueryComponent.builder()
      .filters(new FilterExpression("name", Ops.LIKE_IC,
        expectedEmpName.replace("C", "%").toLowerCase()))
      .build();

    var personDtos = this.personRepository.getAll(qc);
    assertEquals(List.of(expectedEmpName),
      personDtos.resourceList().stream().map( m -> m.getDto().getName()).collect(Collectors.toList())
    );
  }

  @Test
  public void getRestriction_EqualsNull_FiltersOnEqualsNull() {
    Person hasCreatedBy = Person.builder().uuid(UUID.randomUUID()).name(
      TestableEntityFactory.generateRandomNameLettersOnly(7)).createdBy("Created By").build();
    Person noCreatedBy = Person.builder().uuid(UUID.randomUUID())
      .name(TestableEntityFactory.generateRandomNameLettersOnly(7)).build();
    entityManager.persist(hasCreatedBy);
    entityManager.persist(noCreatedBy);

    QueryComponent qc = QueryComponent.builder()
      .filters(new FilterExpression("createdBy", Ops.EQ, null))
      .build();

    var personDtos = this.personRepository.getAll(qc);
    assertEquals(List.of(noCreatedBy.getName()),
      personDtos.resourceList().stream().map( r -> r.getDto().getName()).collect(Collectors.toList()));
  }

  @Test
  public void getRestriction_EqualsNotNull_FiltersOnEqualsNotNull() {
    Person hasCreatedBy = Person.builder().uuid(UUID.randomUUID()).name(
      TestableEntityFactory.generateRandomNameLettersOnly(7)).createdBy("Created By").build();
    Person noCreatedBy = Person.builder().uuid(UUID.randomUUID())
      .name(TestableEntityFactory.generateRandomNameLettersOnly(7)).build();
    entityManager.persist(hasCreatedBy);
    entityManager.persist(noCreatedBy);

    QueryComponent qc = QueryComponent.builder()
      .filters(new FilterExpression("createdBy", Ops.NE, null))
      .build();

    var personDtos = this.personRepository.getAll(qc);
    assertEquals(List.of(hasCreatedBy.getName()),
      personDtos.resourceList().stream().map( r -> r.getDto().getName()).collect(Collectors.toList()));
  }

  @Test
  public void getRestriction_whenFieldIsUuid_filtersByUuid() {
    Person person1 = Person.builder().uuid(UUID.randomUUID()).name("person1").build();
    Person person2 = Person.builder().uuid(UUID.randomUUID()).name("person2").build();
    entityManager.persist(person1);
    entityManager.persist(person2);

    // Filter by uuid
    QueryComponent qc = QueryComponent.builder()
      .filters(new FilterExpression("uuid", Ops.EQ,  person1.getUuid().toString()))
      .build();

    var personDtos = this.personRepository.getAll(qc);
    assertEquals(List.of(person1.getUuid()),
      personDtos.resourceList().stream().map(m -> m.getDto().getUuid()).collect(Collectors.toList())
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

    // Filter by offsetDateTime:
    QueryComponent qc = QueryComponent.builder()
      .filters(new FilterExpression("createdOn", Ops.EQ, creationDateTime.toString()))
      .build();

    var personDtos = this.personRepository.getAll(qc);
    assertEquals(
      Arrays.asList(person1.getCreatedOn(), person2.getCreatedOn()),
      personDtos.resourceList().stream().map( m -> m.getDto().getCreatedOn()).collect(Collectors.toList())
    );
  }
}
