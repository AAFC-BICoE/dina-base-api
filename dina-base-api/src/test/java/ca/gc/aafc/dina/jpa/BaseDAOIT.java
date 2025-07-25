package ca.gc.aafc.dina.jpa;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import java.util.stream.Stream;
import javax.inject.Inject;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;

import ca.gc.aafc.dina.BasePostgresItContext;
import ca.gc.aafc.dina.entity.Employee;
import ca.gc.aafc.dina.entity.Person;
import ca.gc.aafc.dina.testsupport.DatabaseSupportService;
import ca.gc.aafc.dina.testsupport.factories.TestableEntityFactory;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.annotations.NaturalId;
import org.junit.jupiter.api.Test;

import ca.gc.aafc.dina.entity.Department;
import ca.gc.aafc.dina.entity.DepartmentType;

@Transactional
public class BaseDAOIT extends BasePostgresItContext {
  
  @Inject
  private BaseDAO baseDAO;

  @Inject
  private DatabaseSupportService dbSupport;
  
  @Test
  public void getFieldName_onCallWithEntityClass_returnRightFieldName() {
    assertEquals("id", baseDAO.getDatabaseIdFieldName(Department.class));
    assertEquals("uuid", baseDAO.getNaturalIdFieldName(Department.class));

    // make sure it is working if the NaturalId is on the super class
    assertEquals("uuid", baseDAO.getNaturalIdFieldName(Department2.class));

    // make sure it is working if the NaturalId is on the getter
    assertEquals("uuid", baseDAO.getNaturalIdFieldName(Department3.class));
  }

  static class Department2 extends Department {}

  static class Department3 {
    private UUID uuid;

    @NaturalId
    public UUID getUuid(){
      return null;
    }
  }
  
  @Test
  public void findOne_onValidIdentifier_returnsEntity() {
    Department dep = Department.builder().name("dep1").uuid(UUID.randomUUID()).location("dep location").build();
    
    baseDAO.create(dep);
    
    Integer generatedId = dep.getId();
    UUID generatedUUID = dep.getUuid();
    assertNotNull(generatedId);
    
    assertNotNull(baseDAO.findOneByDatabaseId(generatedId, Department.class));
    assertNotNull(baseDAO.findOneByNaturalId(generatedUUID, Department.class));
  }
  
  @Test
  public void findOneByProperty_onValidProperty_returnsEntity() {
    
    Department dep = Department.builder().name("dep1").uuid(UUID.randomUUID()).location("dep location").build();
    baseDAO.create(dep);
    
    Department dep2 = Department.builder().name("dep2").uuid(UUID.randomUUID()).location("dep2 location").build();
    baseDAO.create(dep2);

    Integer generatedId = dep2.getId();
    
    assertEquals(generatedId, baseDAO.findOneByProperty(Department.class, "name", "dep2").getId());
    assertNotEquals(generatedId, baseDAO.findOneByProperty(Department.class, "name", "dep1").getId());
  }

  @Test
  public void findOneByProperties_onValidProperties_returnsEntity() {

    Department dep = Department.builder().name("Dep_Prop").uuid(UUID.randomUUID()).location("Dep_Location").build();
    baseDAO.create(dep);

    Department dep2 = Department.builder().name("Dep_Prop").uuid(UUID.randomUUID()).location("Dep_Location2").build();
    baseDAO.create(dep2);

    Integer generatedId = dep.getId();

    assertEquals(generatedId, baseDAO.findOneByProperties(Department.class,
        List.of(Pair.of("name", "Dep_Prop"), Pair.of("location", "Dep_Location"))).getId());
  }

  @Test
  public void findByProperty_onValidProperty_returnsEntities() {
    String departmentRandomName = TestableEntityFactory.generateRandomNameLettersOnly(7);
    baseDAO.create(Department.builder().name(departmentRandomName)
        .uuid(UUID.randomUUID())
        .location("dep location").build());
    baseDAO.create(Department.builder().name(departmentRandomName)
        .uuid(UUID.randomUUID())
        .location("dep2 location").build());

    assertEquals(2, baseDAO.findByProperty(Department.class, "name", departmentRandomName).size());
    assertEquals(0, baseDAO.findByProperty(Department.class, "name", TestableEntityFactory.generateRandomNameLettersOnly(7)).size());
  }

  @Test
  public void existsByProperty_onValidProperty_existsReturnCorrectValue() {
    Department dep = Department.builder().name("dep1")
            .uuid(UUID.randomUUID())
            .location("dep location").build();
    baseDAO.create(dep);

    Department dep3 = Department.builder().name("dep3")
            .uuid(UUID.randomUUID())
            .location("dep location3").build();
    baseDAO.create(dep3);

    Department dep2 = Department.builder().name("dep9999")
            .uuid(UUID.randomUUID())
            .location("dep location2").build();
    baseDAO.create(dep2);

    assertTrue(baseDAO.existsByProperty(Department.class, "name", "dep9999"));
    assertFalse(baseDAO.existsByProperty(Department.class, "name", "dep8888"));
  }
  
  @Test
  public void setRelationshipUsing_onExistingIdentifier_relationshipIsSet () {
    Department dep = Department.builder().name("dep1")
            .uuid(UUID.randomUUID())
            .location("dep location").build();
    baseDAO.create(dep);
    
    DepartmentType depType = DepartmentType.builder().name("type1").uuid(UUID.randomUUID()).build();
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
    Department dep = Department.builder().name("dep1").uuid(UUID.randomUUID()).location("dep location").build();
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

    Department dep = Department.builder().name("dep1").uuid(UUID.randomUUID()).location("dep location").build();
    baseDAO.create(dep);

    dep.setName(expectedName);
    dep.setLocation(expectedLocation);
    baseDAO.update(dep);

    Department result = baseDAO.findOneByNaturalId(dep.getUuid(), Department.class);
    assertEquals(expectedName, result.getName());
    assertEquals(expectedLocation, result.getLocation());
  }

  // not working atm
//  @Test
//  public void create_InvalidEntity_ExceptionThrownWhenFlush() {
//
//    Department newInvalidDep = Department.builder().uuid(null).build();
//    baseDAO.create(newInvalidDep);
//
//    // at the point the INSERT is created but not flushed so the exception would be reported later
//    // in test it would simply not be reported since the transaction would be rolledback
//
//    //create a new one
//    Department newInvalidDep2 = Department.builder().build();
//    assertThrows(PersistenceException.class, () -> baseDAO.create(newInvalidDep2, true));
//  }

  @Test
  public void refresh_OnRefresh_EntityReloaded() {
    final Department dep = Department.builder().name("depToBeRefreshed").uuid(UUID.randomUUID()).location("dep location").build();

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
    final Department dep = Department.builder().name("depToBeRefreshed").uuid(UUID.randomUUID()).location("dep location").build();

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

  @Test
  public void resultListFromCriteria_withQueryHint_lazyRelEagerlyLoaded() {
    Department dep = Department.builder()
      .name("dep1")
      .uuid(UUID.randomUUID())
      .location("dep location")
      .build();
    baseDAO.create(dep, true);

    Employee emp = Employee.builder().name("abc").department(dep).uuid(UUID.randomUUID()).build();
    baseDAO.create(emp, true);

    //detach the entity to force reload
    baseDAO.detach(dep);
    baseDAO.detach(emp);

    // Build a criteria to load the Department by UUID
    CriteriaBuilder criteriaBuilder = baseDAO.getCriteriaBuilder();
    CriteriaQuery<Department> criteria = criteriaBuilder.createQuery(Department.class);
    Root<Department> root = criteria.from(Department.class);
    Predicate clause = criteriaBuilder.equal(root.get("uuid"), dep.getUuid());
    criteria.where(clause).select(root);

    // Load the department without hints
    List<Department> depList = baseDAO.resultListFromCriteria(criteria,0,10);
    assertFalse(depList.isEmpty());
    // Test that the relationship is not loaded. It means Hibernate would need to make a new call to the db to load it
    assertFalse(baseDAO.isLoaded(depList.get(0), "employees"));
    // Force a lazy-load to make sure it exists
    assertNotNull(depList.get(0).getEmployees().get(0));

    //detach the entity to force reload
    baseDAO.detach(dep);
    baseDAO.detach(emp);

    // Load the department with loadgraph hint
    depList = baseDAO.resultListFromCriteria(criteria,0,10, Map.of(BaseDAO.LOAD_GRAPH_HINT_KEY,
      baseDAO.createEntityGraph(Department.class,"employees")));
    // make sure the relationship is loaded
    assertTrue(baseDAO.isLoaded(depList.get(0), "employees"));
    assertNotNull(depList.get(0).getEmployees().get(0));
  }

  @Test
  public void resultListFromCriteria_withNestedQueryHint_lazyRelEagerlyLoaded() {
    Department department = Department.builder()
        .name("nestedDepartment")
        .uuid(UUID.randomUUID())
        .location("nested dep location")
        .build();
    baseDAO.create(department, true);

    Person nestedManager = Person.builder()
        .uuid(UUID.randomUUID())
        .name("John Doe")
        .build();
    baseDAO.create(nestedManager, true);

    Employee emp1 = Employee.builder()
        .name("abc1")
        .uuid(UUID.randomUUID())
        .department(department)
        .manager(nestedManager)
        .build();
    Employee emp2 = Employee.builder()
        .name("abc2")
        .uuid(UUID.randomUUID())
        .department(department)
        .manager(nestedManager)
        .build();
    baseDAO.create(emp1, true);
    baseDAO.create(emp2, true);

    // Detach created test data
    baseDAO.detach(emp1);
    baseDAO.detach(emp2);
    baseDAO.detach(department);
    baseDAO.detach(nestedManager);

    // Build a criteria to load the Department by UUID
    CriteriaBuilder criteriaBuilder = baseDAO.getCriteriaBuilder();
    CriteriaQuery<Department> criteria = criteriaBuilder.createQuery(Department.class);
    Root<Department> root = criteria.from(Department.class);
    Predicate clause = criteriaBuilder.equal(root.get("uuid"), department.getUuid());
    criteria.where(clause).select(root);

    // Load the department without any nested hints provided.
    List<Department> depList = baseDAO.resultListFromCriteria(criteria, 0, 1);
    assertFalse(depList.isEmpty());
    assertFalse(baseDAO.isLoaded(depList.get(0), "employees"));
    assertEquals(2, depList.get(0).getEmployees().size());
    assertFalse(baseDAO.isLoaded(depList.get(0).getEmployees().get(0), "manager"));
    assertNotNull(depList.get(0).getEmployees().get(0).getManager());

    // detach the entities to force reload
    baseDAO.detach(emp1);
    baseDAO.detach(emp2);
    baseDAO.detach(department);
    baseDAO.detach(nestedManager);

    // Load the department with nested hints provided.
    depList = baseDAO.resultListFromCriteria(criteria, 0, 10, Map.of(BaseDAO.LOAD_GRAPH_HINT_KEY,
        baseDAO.createEntityGraph(Department.class, "employees.manager")));
    assertFalse(depList.isEmpty());
    assertTrue(baseDAO.isLoaded(depList.get(0), "employees"));
    assertTrue(baseDAO.isLoaded(depList.get(0).getEmployees().get(0), "manager"));
    assertNotNull(depList.get(0).getEmployees().get(0).getManager());

    // detach the entities to force reload
    baseDAO.detach(depList.get(0));

    // try with findOneByNaturalId
    Department departmentFromFindOne = baseDAO.findOneByNaturalId(department.getUuid(), Department.class, Map.of(BaseDAO.LOAD_GRAPH_HINT_KEY,
      baseDAO.createEntityGraph(Department.class, "employees.manager")));
    assertTrue(baseDAO.isLoaded(departmentFromFindOne, "employees"));
  }

  @Test
  public void findAllByQuery_onValidQuery_expectedResultsReturned() {
    Person p1 = Person.builder().name("abc").uuid(UUID.randomUUID()).build();
    Person p2 = Person.builder().name("bcd").uuid(UUID.randomUUID()).build();

    baseDAO.create(p1, true);
    baseDAO.create(p2, true);

    String sql = "SELECT new " + DinaObjectSummary.class.getCanonicalName() +
      "(t.uuid, t.group, t.createdBy) FROM Person t WHERE name LIKE CONCAT('%',:name,'%') ORDER BY id";

    List<DinaObjectSummary> objList =
      baseDAO.findAllByQuery(DinaObjectSummary.class, sql, List.of(Pair.of("name", "bc")), 10, 0);
    assertEquals(2, objList.size());
  }

  @Test
  public void streamAllByQuery_onValidQuery_expectedResultsReturned() {
    for (int i = 0; i < 1000; i++) {
      Person p1 = Person.builder().name(RandomStringUtils.randomAlphabetic(8)).uuid(UUID.randomUUID()).build();
      baseDAO.create(p1, true);
      baseDAO.detach(p1);
    }
    String sql = "SELECT new " + DinaObjectSummary.class.getCanonicalName() +
      "(t.uuid, t.group, t.createdBy) FROM Person t ORDER BY id";
    Stream<DinaObjectSummary> objStream =
      baseDAO.streamAllByQuery(DinaObjectSummary.class, sql, null);
    assertEquals(1000, objStream.count());
  }
}
