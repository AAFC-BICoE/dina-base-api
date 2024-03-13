package ca.gc.aafc.dina.repository;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.entity.Person;
import ca.gc.aafc.dina.service.ResourceNameIdentifierService;
import ca.gc.aafc.dina.testsupport.PostgresTestContainerInitializer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.UUID;
import javax.inject.Inject;
import javax.transaction.Transactional;

@Transactional
@SpringBootTest(classes = TestDinaBaseApp.class)
@ContextConfiguration(initializers = { PostgresTestContainerInitializer.class })
public class ResourceIdBaseRepositoryTest {

  @Inject
  private DinaRepositoryIT.DinaPersonService personService;

  @Inject
  private ResourceNameIdentifierService resourceNameIdentifierService;

  @Test
  public void a() {

    Person person = Person.builder()
      .name("xyz abc")
      .group("g1")
      .build();
    UUID uuid = personService.createAndFlush(person).getUuid();

    ResourceNameIdentifierBaseRepository repo = new ResourceNameIdentifierBaseRepository(resourceNameIdentifierService,
      Map.of("person", Person.class));
    UUID foundUUID = repo.findOne("filter[type][EQ]=person&filter[name][EQ]=xyz abc&filter[group][EQ]=g1");

    assertEquals(uuid, foundUUID);

  }
}
