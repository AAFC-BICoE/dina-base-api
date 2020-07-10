package ca.gc.aafc.dina.testsupport;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import ca.gc.aafc.dina.testsupport.entity.ComplexObject;

@SpringBootTest(classes = TestConfiguration.class)
@Transactional
@ActiveProfiles("test")
public class DBBackedIntegrationTestTest {

  @Inject
  private DatabaseSupportService service;

  @Test
  public void deleteById_GivenExistingId_DeletesEntity() {
    ComplexObject comp = ComplexObject.builder().name("name").build();
    
    service.save(comp);

    service.deleteById(ComplexObject.class, comp.getId());
    assertNull(service.find(ComplexObject.class, comp.getId()));
  }
  
  @Test
  public void deleteByProperty_NotInNewTransaction_EntityDeletes() {
    ComplexObject comp = persistEntity();

    service.deleteByProperty(ComplexObject.class, "id", comp.getId());
    assertNull(service.find(ComplexObject.class, comp.getId()));
  }

  @Test
  public void deleteByProperty_InNewTransaction_EntityDeletes() {
    ComplexObject comp = persistEntity();

    service.deleteByProperty(ComplexObject.class, "id", comp.getId(), true);
    assertNull(service.find(ComplexObject.class, comp.getId()));
  }

  @Test
  public void deleteByProperty_MultipleEntities_AllEntitiesDelete() {
    List<ComplexObject> persistedEntities = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      ComplexObject comp = persistEntity();
      persistedEntities.add(comp);
    }

    // Delete all entities with same name
    service.deleteByProperty(ComplexObject.class, "name", persistedEntities.get(0).getName());
    persistedEntities.forEach(pe -> assertNull(service.find(ComplexObject.class, pe.getId())));
  }

  private ComplexObject persistEntity() {
    ComplexObject comp = ComplexObject.builder().name("name").build();
    service.runInNewTransaction(em -> em.persist(comp));
    assertNotNull(comp.getId());
    return comp;
  }
}
