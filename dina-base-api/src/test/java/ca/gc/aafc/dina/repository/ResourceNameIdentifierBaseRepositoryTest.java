package ca.gc.aafc.dina.repository;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.entity.Person;
import ca.gc.aafc.dina.service.ResourceNameIdentifierService;
import ca.gc.aafc.dina.testsupport.PostgresTestContainerInitializer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import java.util.UUID;
import javax.inject.Inject;
import javax.transaction.Transactional;

@Transactional
@SpringBootTest(classes = TestDinaBaseApp.class)
@ContextConfiguration(initializers = { PostgresTestContainerInitializer.class })
public class ResourceNameIdentifierBaseRepositoryTest {

  @Inject
  private DinaRepositoryIT.DinaPersonService personService;

  @Inject
  private ResourceNameIdentifierService resourceNameIdentifierService;

  @Test
  public void findOne_onValidQuery_identifierReturned() {

    Person person = Person.builder()
      .name("xyz abc")
      .group("g1")
      .build();
    UUID uuid = personService.createAndFlush(person).getUuid();

    ResourceNameIdentifierBaseRepository repo = new ResourceNameIdentifierBaseRepository(resourceNameIdentifierService,
      Map.of("person", Person.class));
    Pair<String, UUID> foundIdentifier = repo.findOne("filter[type][EQ]=person&filter[name][EQ]=xyz abc&filter[group][EQ]=g1");

    assertEquals("xyz abc", foundIdentifier.getKey());
    assertEquals(uuid, foundIdentifier.getValue());
  }

  @Test
  public void findOne_onInvalidQuery_exceptionThrown() {
    ResourceNameIdentifierBaseRepository repo = new ResourceNameIdentifierBaseRepository(resourceNameIdentifierService,
      Map.of("person", Person.class));

    assertThrows(IllegalArgumentException.class, () -> repo.findOne("filter"));
  }
}
