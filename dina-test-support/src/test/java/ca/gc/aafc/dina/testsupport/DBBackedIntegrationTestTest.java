package ca.gc.aafc.dina.testsupport;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import javax.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import ca.gc.aafc.dina.testsupport.entity.ComplexObject;

@SpringBootTest(classes = TestConfiguration.class)
@Transactional
@ActiveProfiles("test")
public class DBBackedIntegrationTestTest extends DBBackedIntegrationTest {

  @Test
  public void deleteByProperty_NotInNewTransaction_EntityDeletes() {
    ComplexObject comp = persistEntity();

    deleteByProperty(ComplexObject.class, "id", comp.getId());
    assertNull(find(ComplexObject.class, comp.getId()));
  }

  @Test
  public void deleteByProperty_InNewTransaction_EntityDeletes() {
    ComplexObject comp = persistEntity();

    deleteByProperty(ComplexObject.class, "id", comp.getId(), true);
    assertNull(find(ComplexObject.class, comp.getId()));
  }

  private ComplexObject persistEntity() {
    ComplexObject comp = ComplexObject.builder().name("name").build();
    runInNewTransaction(em -> em.persist(comp));
    assertNotNull(comp.getId());
    return comp;
  }
}
