package ca.gc.aafc.dina.jpa;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.transaction.Transactional;

import ca.gc.aafc.dina.testsupport.DatabaseSupportService;
import ca.gc.aafc.dina.testsupport.factories.TestableEntityFactory;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.entity.Department;
import ca.gc.aafc.dina.entity.DepartmentType;

@Transactional
@SpringBootTest(classes = TestDinaBaseApp.class)
public class BaseDAOIT {
  
  @Inject
  private BaseDAO baseDAO;

  @Inject
  private DatabaseSupportService dbSupport;
  
  @Test
  public void getFieldName_onCallWithEntityClass_returnRightFieldName() {
    assertEquals("id", baseDAO.getDatabaseIdFieldName(Department.class));
    assertEquals("uuid", baseDAO.getNaturalIdFieldName(Department.class));
  }
  
  @Test
  public void findOne_onValidIdentifier_returnsEntity() {
    Department dep = Department.builder().name("dep1").location("dep location").build();
    
    baseDAO.create(dep);
    
    Integer generatedId = dep.getId();
    UUID generatedUUID = dep.getUuid();
    assertNotNull(generatedId);
    
    assertNotNull(baseDAO.findOneByDatabaseId(generatedId, Department.class));
    assertNotNull(baseDAO.findOneByNaturalId(generatedUUID, Department.class));
  }
  
  @Test
  public void findOneByProperty_onValidProperty_returnsEntity() {
    
    Department dep = Department.builder().name("dep1").location("dep location").build();
    baseDAO.create(dep);
    
    Department dep2 = Department.builder().name("dep2").location("dep2 location").build();
    baseDAO.create(dep2);

    Integer generatedId = dep2.getId();
    
    assertEquals(generatedId, baseDAO.findOneByProperty(Department.class, "name", "dep2").getId());
    assertNotEquals(generatedId, baseDAO.findOneByProperty(Department.class, "name", "dep1").getId());
  }

  @Test
  public void findOneByProperties_onValidProperties_returnsEntity() {

    Department dep = Department.builder().name("Dep_Prop").location("Dep_Location").build();
    baseDAO.create(dep);

    Department dep2 = Department.builder().name("Dep_Prop").location("Dep_Location2").build();
    baseDAO.create(dep2);

    Integer generatedId = dep.getId();

    assertEquals(generatedId, baseDAO.findOneByProperties(Department.class,
        List.of(Pair.of("name", "Dep_Prop"), Pair.of("location", "Dep_Location"))).getId());
  }

  @Test
  public void findByProperty_onValidProperty_returnsEntities() {
    String departmentRandomName = TestableEntityFactory.generateRandomNameLettersOnly(7);
    baseDAO.create(Department.builder().name(departmentRandomName)
        .location("dep location").build());
    baseDAO.create(Department.builder().name(departmentRandomName)
        .location("dep2 location").build());

    assertEquals(2, baseDAO.findByProperty(Department.class, "name", departmentRandomName).size());
    assertEquals(0, baseDAO.findByProperty(Department.class, "name", TestableEntityFactory.generateRandomNameLettersOnly(7)).size());
  }

  @Test
  public void existsByProperty_onValidProperty_existsReturnCorrectValue() {
    Department dep = Department.builder().name("dep1").location("dep location").build();
    baseDAO.create(dep);

    Department dep3 = Department.builder().name("dep3").location("dep location3").build();
    baseDAO.create(dep3);

    Department dep2 = Department.builder().name("dep9999").location("dep location2").build();
    baseDAO.create(dep2);

    assertTrue(baseDAO.existsByProperty(Department.class, "name", "dep9999"));
    assertFalse(baseDAO.existsByProperty(Department.class, "name", "dep8888"));
  }
  
  @Test
  public void setRelationshipUsing_onExistingIdentifier_relationshipIsSet () {
    Department dep = Department.builder().name("dep1").location("dep location").build();
    baseDAO.create(dep);
    
    DepartmentType depType = DepartmentType.builder().name("type1").build();
    baseDAO.create(depType);
    
    UUID depTypeUUID = depType.getUuid();
    
    baseDAO.setRelationshipByNaturalIdReference(DepartmentType.class, depTypeUUID,
        dep::setDepartmentType);
    
    Department reloadedDep = baseDAO.findOneByDatabaseId(dep.getId(), Department.class);
    assertNotNull(reloadedDep.getDepartmentType());
    assertEquals("type1", reloadedDep.getDepartmentType().getName());
  }

  @Test
  public void createDelete_onCreateAndDelete_entitySavedAndDeleted() {
    Department dep = Department.builder().name("dep1").location("dep location").build();
    baseDAO.create(dep);

    Integer generatedId = dep.getId();
    assertNotNull(generatedId);

    baseDAO.delete(dep);

    assertNull(baseDAO.findOneByDatabaseId(generatedId, Department.class));
  }

  @Test
  public void update_OnUpdate_EntityUpdated() {
    String expectedName = RandomStringUtils.random(5);
    String expectedLocation = RandomStringUtils.random(5);

    Department dep = Department.builder().name("dep1").location("dep location").build();
    baseDAO.create(dep);

    dep.setName(expectedName);
    dep.setLocation(expectedLocation);
    baseDAO.update(dep);

    Department result = baseDAO.findOneByNaturalId(dep.getUuid(), Department.class);
    assertEquals(expectedName, result.getName());
    assertEquals(expectedLocation, result.getLocation());
  }

  @Test
  public void create_InvalidEntity_ExceptionThrownWhenFlush() {

    // since the test is using H2 and baseDAO directly we need to set the constraint explicitly
    dbSupport.runInNewTransaction( em -> {
      Query q = em.createNativeQuery("ALTER TABLE DEPARTMENT ALTER COLUMN LOCATION SET NOT NULL;");
      q.executeUpdate();
    });

    Department newInvalidDep = Department.builder().build();
    baseDAO.create(newInvalidDep);

    // at the point the INSERT is created but not flushed so the exception would be reported later
    // in test it would simply not be reported since the transaction would be rolledback

    //create a new one
    Department newInvalidDep2 = Department.builder().build();
    assertThrows(PersistenceException.class, () -> baseDAO.create(newInvalidDep2, true));
  }

  @Test
  public void refresh_OnRefresh_EntityReloaded() {
    final Department dep = Department.builder().name("depToBeRefreshed").location("dep location").build();

    // add an entity in another transaction so it exists in the database outside of the test's transaction
    dbSupport.runInNewTransaction( em -> em.persist(dep));
    Integer depId = dep.getId();
    assertNotNull(depId);

    // load the entity from the database
    Department departmentUnderTest = baseDAO.findOneByDatabaseId(depId, Department.class);
    assertNotNull(departmentUnderTest);

    // change the entity outside the test's context
    dbSupport.runInNewTransaction( (em -> {
      Department outsideTransactionDep = em.find(Department.class, depId);
      outsideTransactionDep.setName("depChanged");
    }));

    // validate that the name is not changed
    assertEquals("depToBeRefreshed", departmentUnderTest.getName());
    // the name is not changed even if we ask to find it again
    departmentUnderTest = baseDAO.findOneByDatabaseId(depId, Department.class);
    assertEquals("depToBeRefreshed", departmentUnderTest.getName());

    // this is what refresh will do
    baseDAO.refresh(departmentUnderTest);
    assertEquals("depChanged", departmentUnderTest.getName());
  }

  @Test
  public void detach_OnDetach_FindReloadTheEntity() {
    final Department dep = Department.builder().name("depToBeRefreshed").location("dep location").build();

    // add an entity in another transaction so it exists in the database outside of the test's transaction
    dbSupport.runInNewTransaction( em -> em.persist(dep));
    Integer depId = dep.getId();
    assertNotNull(depId);

    // load the entity from the database
    Department departmentUnderTest = baseDAO.findOneByDatabaseId(depId, Department.class);
    assertNotNull(departmentUnderTest);

    // change the entity outside the test's context
    dbSupport.runInNewTransaction( (em -> {
      Department outsideTransactionDep = em.find(Department.class, depId);
      outsideTransactionDep.setName("depChanged");
    }));

    // validate that the name is not changed
    assertEquals("depToBeRefreshed", departmentUnderTest.getName());

    // tell JPA to detach the object since we are not sure it's still up-to-date
    baseDAO.detach(departmentUnderTest);

    // then find will reload it from the database
    departmentUnderTest = baseDAO.findOneByDatabaseId(depId, Department.class);
    assertEquals("depChanged", departmentUnderTest.getName());
  }

}
