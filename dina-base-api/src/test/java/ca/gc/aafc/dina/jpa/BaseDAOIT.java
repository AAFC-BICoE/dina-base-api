package ca.gc.aafc.dina.jpa;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.ConstraintViolationException;

import org.apache.commons.lang3.RandomStringUtils;
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
  public void setRelationshipUsing_onExistingIdentifer_relationshipIsSet () {
    Department dep = Department.builder().name("dep1").location("dep location").build();
    baseDAO.create(dep);
    
    DepartmentType depType = DepartmentType.builder().name("type1").build();
    baseDAO.create(depType);
    
    UUID depTypeUUID = depType.getUuid();
    
    baseDAO.setRelationshipByNaturalIdReference(DepartmentType.class, depTypeUUID,
        (x) -> dep.setDepartmentType(x));
    
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
  public void update_onValidationError_throwConstraintViolationException() {
    Department dep = Department.builder().name("dep1").location("dep location").build();
    baseDAO.create(dep);

    dep.setLocation(null);

    assertThrows(
      ConstraintViolationException.class,
      () -> baseDAO.update(dep)
    );
  }

}
