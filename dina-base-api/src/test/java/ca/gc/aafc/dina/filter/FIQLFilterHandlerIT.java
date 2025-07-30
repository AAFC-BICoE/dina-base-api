package ca.gc.aafc.dina.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import ca.gc.aafc.dina.entity.Person;
import ca.gc.aafc.dina.testsupport.PostgresTestContainerInitializer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;

/**
 * v2
 */
@SpringBootTest
@ContextConfiguration(initializers = {PostgresTestContainerInitializer.class})
public class FIQLFilterHandlerIT {

  @Inject
  private EntityManager entityManager;

  private static final OffsetDateTime CREATION_DATE_TIME = OffsetDateTime.parse("2013-07-01T17:55:13-07:00");

  @BeforeEach
  public void initEmployees() {
    // Persist 5 test People:
    Person person1 = Person.builder().uuid(UUID.randomUUID()).name("person1").createdOn(CREATION_DATE_TIME).build();
    entityManager.persist(person1);
    entityManager.persist(Person.builder().uuid(UUID.randomUUID()).name("person2").createdOn(CREATION_DATE_TIME).build());
    entityManager.persist(Person.builder().uuid(UUID.randomUUID()).name("person3").createdOn(CREATION_DATE_TIME).build());
    entityManager.persist(Person.builder().uuid(UUID.randomUUID()).name("person4").createdOn(CREATION_DATE_TIME).build());
    entityManager.persist(Person.builder().uuid(UUID.randomUUID()).name("person5").createdOn(CREATION_DATE_TIME).build());
  }

  @Test
  @Transactional
  public void findAllEmployees_whenRsqlFilterIsSet_filteredEmployeesAreReturned() {

    // Check that the 2 persons were returned.
    var q = FIQLFilterHandler.criteriaQuery(entityManager, "name==person1,name==person3",
      Person.class, Person.class);

    List<Person> personList = entityManager.createQuery(q)
      .getResultList();

    assertEquals(2, personList.size());
    assertEquals("person1", personList.get(0).getName());
    assertEquals("person3", personList.get(1).getName());
  }

}
