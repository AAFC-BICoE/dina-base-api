package ca.gc.aafc.dina.service;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.transaction.Transactional;

import com.google.common.collect.ImmutableMap;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import ca.gc.aafc.dina.TestConfiguration;
import ca.gc.aafc.dina.entity.Department;
import ca.gc.aafc.dina.entity.DepartmentType;
import ca.gc.aafc.dina.jpa.BaseDAO;
import ca.gc.aafc.dina.jpa.DinaService;
import lombok.NonNull;

@Transactional
@SpringBootTest(classes = TestConfiguration.class)
public class DinaServiceTest {

  @Inject
  private DinaServiceTestImplementation serviceUnderTest;

  @Test
  public void create_ValidEntity_EntityPersists() {
    Department result = persistDepartment();
    assertNotNull(result.getId());
  }

  @Test
  public void findOne_ValidInput_FindsOne() {
    Department expected = persistDepartment();

    Department result = serviceUnderTest.findOne(expected.getUuid(), Department.class);
    assertEquals(expected.getId(), result.getId());
    assertEquals(expected.getUuid(), result.getUuid());
    assertEquals(expected.getName(), result.getName());
    assertEquals(expected.getLocation(), result.getLocation());
  }

  @Test
  public void findAllWhere_WhereNameIs_FindsAllWithName() {
    String expectedName = RandomStringUtils.random(15);
    int expectedNumberOfEntities = 10;

    persistDepartmentsWithName(expectedName, expectedNumberOfEntities);
    // Persist extra departments to validate correct number returned
    persistDepartmentsWithName(RandomStringUtils.random(15), expectedNumberOfEntities);

    Map<String, Object> where = ImmutableMap.of("name", expectedName);

    List<Department> resultList = serviceUnderTest.findAllWhere(Department.class, where);
    assertEquals(expectedNumberOfEntities, resultList.size());
    resultList.forEach(result -> assertEquals(expectedName, result.getName()));
  }

  @Test
  public void findAllWhere_WhereNull_FindsAllWhereNull() {
    int expectedNumberOfEntities = 10;

    persistDepartmentsWithName(null, expectedNumberOfEntities);
    // Persist extra departments to validate correct number returned
    persistDepartmentsWithName(RandomStringUtils.random(15), expectedNumberOfEntities);

    Map<String, Object> where = new HashMap<>();
    where.put("name", null);

    List<Department> resultList = serviceUnderTest.findAllWhere(Department.class, where);
    assertEquals(expectedNumberOfEntities, resultList.size());
    resultList.forEach(result -> assertNull(result.getName()));
  }

  @Test
  public void findAllWhere_NoWhereMap_FindsAll() {
    int expectedNumberOfEntities = 10;

    persistDepartmentsWithName("name", expectedNumberOfEntities);

    List<Department> resultList = serviceUnderTest.findAllWhere(
      Department.class,
      Collections.<String,Object>emptyMap());
    assertEquals(expectedNumberOfEntities, resultList.size());
  }

  @Test
  public void update_ValidInput_EntityUpdated() {
    String expectedName = RandomStringUtils.random(5);
    String expectedLocation = RandomStringUtils.random(5);

    Department expected = persistDepartment();
    assertNotEquals(expectedName, expected.getName());
    assertNotEquals(expectedLocation, expected.getLocation());

    expected.setName(expectedName);
    expected.setLocation(expectedLocation);
    serviceUnderTest.update(expected);

    Department result = serviceUnderTest.findOne(expected.getUuid(), Department.class);
    assertEquals(expectedName, result.getName());
    assertEquals(expectedLocation, result.getLocation());
  }

  @Test
  public void delete_ValidInput_EntityDeleted() {
    Department result = persistDepartment();

    serviceUnderTest.delete(result);
    assertNull(serviceUnderTest.findOne(result.getUuid(), Department.class));
  }

  @Test
  public void preCreate_SetUuid_RunsBeforeCreate() {
    Department result = persistDepartment();
    assertNotNull(result.getUuid());
  }

  @Test
  public void preUpdate_SetDepartType_RunsBeforeUpdate() {
    Department result = persistDepartment();
    assertNull(result.getDepartmentType());
    serviceUnderTest.update(result);
    assertNotNull(result.getDepartmentType());
  }

  @Test
  public void preDelete_SetNameAndLocationNull_RunsBeforeDelete() {
    Department result = persistDepartment();
    assertNotNull(result.getName());
    assertNotNull(result.getLocation());
    serviceUnderTest.delete(result);
    assertNull(result.getName());
    assertNull(result.getLocation());
  }

  private Department persistDepartment() {
    Department result = createDepartment();
    assertNull(result.getId());
    assertNull(result.getUuid());
    serviceUnderTest.create(result);
    return result;
  }

  private static Department createDepartment() {
    return Department
      .builder()
      .name(RandomStringUtils.random(5))
      .location(RandomStringUtils.random(5))
      .build();
  }

  private void persistDepartmentsWithName(String name, int numToPersist) {
    for (int i = 0; i < numToPersist; i++) {
      Department toPersist = createDepartment();
      toPersist.setName(name);
      serviceUnderTest.create(toPersist);
    }
  }

  public static class DinaServiceTestImplementation extends DinaService<Department> {

    @Inject
    private BaseDAO baseDAO;

    public DinaServiceTestImplementation(@NonNull BaseDAO baseDAO) {
      super(baseDAO);
    }

    @Override
    protected Department preCreate(Department entity) {
      entity.setUuid(UUID.randomUUID());
      return entity;
    }

    @Override
    protected Department preUpdate(Department entity) {
      DepartmentType type = DepartmentType.builder().name("name").build();
      baseDAO.create(type);
      entity.setDepartmentType(type);
      return entity;
    }

    @Override
    protected void preDelete(Department entity) {
      entity.setName(null);
      entity.setLocation(null);
    }

  }

}
