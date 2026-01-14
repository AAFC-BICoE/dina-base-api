package ca.gc.aafc.dina.repository;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.config.PersonTestConfig;
import ca.gc.aafc.dina.entity.Person;
import ca.gc.aafc.dina.security.auth.GroupWithReadAuthorizationService;
import ca.gc.aafc.dina.service.NameUUIDPair;
import ca.gc.aafc.dina.service.ResourceNameIdentifierService;
import ca.gc.aafc.dina.testsupport.PostgresTestContainerInitializer;
import ca.gc.aafc.dina.testsupport.factories.TestableEntityFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.inject.Inject;
import javax.transaction.Transactional;

@Transactional
@SpringBootTest(classes = TestDinaBaseApp.class)
@ContextConfiguration(initializers = { PostgresTestContainerInitializer.class })
@Import(PersonTestConfig.class)
public class ResourceNameIdentifierBaseRepositoryTest {

  @Inject
  private PersonTestConfig.DinaPersonService personService;

  @Inject
  private GroupWithReadAuthorizationService authorizationService;

  @Inject
  private ResourceNameIdentifierService resourceNameIdentifierService;

  @Test
  public void findOne_onValidQuery_identifierReturned() {

    Person person = Person.builder()
      .name("xyz cba")
      .group("g1")
      .build();
    UUID uuid1 = personService.createAndFlush(person).getUuid();

    ResourceNameIdentifierBaseRepository repo = new ResourceNameIdentifierBaseRepository(resourceNameIdentifierService,
      authorizationService, Map.of("person", Person.class));
    NameUUIDPair result = repo.findOne("filter[type][EQ]=person&filter[name][EQ]=xyz cba&filter[group][EQ]=g1");
    assertEquals("xyz cba", result.name());
    assertEquals(uuid1, result.uuid());
  }

  @Test
  public void findAll_onValidQuery_identifierReturned() {

    Person person = Person.builder()
      .name("xyz abc")
      .group("g1")
      .build();
    UUID uuid1 = personService.createAndFlush(person).getUuid();
    Person person2 = Person.builder()
      .name("fdas 423")
      .group("g1")
      .build();
    UUID uuid2 = personService.createAndFlush(person2).getUuid();

    ResourceNameIdentifierBaseRepository repo = new ResourceNameIdentifierBaseRepository(resourceNameIdentifierService,
      authorizationService, Map.of("person", Person.class));
    List<NameUUIDPair> result = repo.findAll("filter[type][EQ]=person&filter[name][EQ]=xyz abc,fdas 423&filter[group][EQ]=g1");
    assertEquals(2, result.size());
  }

  @Test
  public void listAll_onValidQuery_identifierReturned() {

    Person person = Person.builder()
      .name(TestableEntityFactory.generateRandomNameLettersOnly(8))
      .group("g1")
      .build();
    UUID uuid1 = personService.createAndFlush(person).getUuid();
    Person person2 = Person.builder()
      .name(TestableEntityFactory.generateRandomNameLettersOnly(8))
      .group("g1")
      .build();
    UUID uuid2 = personService.createAndFlush(person2).getUuid();

    ResourceNameIdentifierBaseRepository repo = new ResourceNameIdentifierBaseRepository(resourceNameIdentifierService,
      authorizationService, Map.of("person", Person.class));
    List<NameUUIDPair> result = repo.findAll("filter[type][EQ]=person&filter[group][EQ]=g1");
    assertEquals(2, result.size());
  }

  @Test
  public void findOne_onInvalidQuery_exceptionThrown() {
    ResourceNameIdentifierBaseRepository repo = new ResourceNameIdentifierBaseRepository(resourceNameIdentifierService,
      authorizationService, Map.of("person", Person.class));

    assertThrows(IllegalArgumentException.class, () -> repo.findOne("filter"));
  }
}
