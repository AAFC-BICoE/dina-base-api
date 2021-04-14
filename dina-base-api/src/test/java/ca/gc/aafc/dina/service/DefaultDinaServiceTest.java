package ca.gc.aafc.dina.service;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.ValidationException;

import org.apache.commons.lang3.RandomStringUtils;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.entity.Department;
import ca.gc.aafc.dina.entity.DepartmentType;
import ca.gc.aafc.dina.jpa.BaseDAO;
import lombok.NonNull;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;

@Transactional
@SpringBootTest(classes = TestDinaBaseApp.class)
public class DefaultDinaServiceTest {

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
  public void findAllWhere_EmptyWhereClause_FindsAll() {
    int expectedNumberOfEntities = 10;
    Set<UUID> expecteUuids = new HashSet<>();

    for (int i = 0; i < expectedNumberOfEntities; i++) {
      expecteUuids.add(persistDepartment().getUuid());
    }

    Set<UUID> resultIds = serviceUnderTest
      .findAll(Department.class, (cb, root) -> new Predicate[] {}, null, 0, 100)
      .stream()
      .map(Department::getUuid)
      .collect(Collectors.toSet());

    assertThat(expecteUuids, CoreMatchers.is(resultIds));
  }

  @Test
  public void findAllWhere_WhereClause_FindsAllWhere() {
    int expectedNumberOfEntities = 10;
    String expectedName = RandomStringUtils.random(10);
    Set<UUID> expecteUuids = new HashSet<>();

    for (int i = 0; i < expectedNumberOfEntities; i++) {
      Department dept = createDepartment();
      dept.setName(expectedName);
      serviceUnderTest.create(dept);
      expecteUuids.add(dept.getUuid());
      //Persist extras
      persistDepartment();
    }

    BiFunction<CriteriaBuilder, Root<Department>, Predicate[]> where =
        (cb, root) -> new Predicate[] { cb.equal(root.get("name"), expectedName) };

    Set<UUID> resultIds = serviceUnderTest
      .findAll(Department.class, where, null, 0, 100)
      .stream()
      .map(Department::getUuid)
      .collect(Collectors.toSet());

    assertThat(expecteUuids, CoreMatchers.is(resultIds));
  }

  @Test
  public void findAllWhere_OrderBySupplied_OrdersBy() {
    List<String> names = Arrays.asList("a", "b", "c", "d");
    List<String> shuffledNames = Arrays.asList("b", "a", "d", "c");

    for (String name : shuffledNames) {
      Department dept = createDepartment();
      dept.setName(name);
      serviceUnderTest.create(dept);
    }

    List<Department> resultList = serviceUnderTest
      .findAll(
        Department.class,
        (cb, root) -> new Predicate[] {}, 
        (cb, root) -> Arrays.asList(cb.asc(root.get("name"))), 0, 100);

    for (int i = 0; i < names.size(); i++) {
      assertEquals(names.get(i), resultList.get(i).getName());
    }
  }

   @Test
  public void findAllWhere_UsingStartIndex_FindsAllAtIndex() {
    int skip = 5;
    List<UUID> expecteUuids = new ArrayList<>();

    for (int i = 0; i < 10; i++) {
      expecteUuids.add(persistDepartment().getUuid());
    }
    expecteUuids = expecteUuids.subList(skip, expecteUuids.size());

    List<UUID> resultIds = serviceUnderTest
      .findAll(Department.class, (cb, root) -> new Predicate[] {}, null, skip, 100)
      .stream()
      .map(Department::getUuid)
      .collect(Collectors.toList());

    assertThat(expecteUuids, CoreMatchers.is(resultIds));
  }

  @Test
  public void findAllWhere_MaxResults_LimitsResults() {
    int maxResults = 5;

    for (int i = 0; i < 10; i++) {
     persistDepartment();
    }

    List<Department> resultsList = serviceUnderTest
      .findAll(Department.class, (cb, root) -> new Predicate[] {}, null, 0, maxResults);

    assertEquals(maxResults, resultsList.size());
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

  @Test
  public void exists_whenRecordExists_returnsTrue() {
    Department result = persistDepartment();
    assertTrue(serviceUnderTest.exists(Department.class, result.getUuid()));
    serviceUnderTest.delete(result);
    assertFalse(serviceUnderTest.exists(Department.class, result.getUuid()));
  }

  @Test
  public void exists_whenRecordDoesNotExist_returnsFalse() {
    assertFalse(serviceUnderTest.exists(Department.class, UUID.randomUUID()));
  }

  @Test
  public void validationGroups_DepartmentWithNonNullUuidOnCreate_ThrowsException() {
    Department result = createDepartment();
    result.setUuid(UUID.randomUUID());
    ConstraintViolationException exception = assertThrows(ConstraintViolationException.class, () -> {
      serviceUnderTest.create(result);
    });

    String expectedMessage = "must be null";
    String actualMessage = exception.getMessage();

    assertTrue(actualMessage.contains(expectedMessage));
  }

  @Test
  public void validationGroups_DepartmentWithNullUuidOnUpdate_ThrowException() {
    Department result = persistDepartment();
    result.setUuid(null);
    ConstraintViolationException exception =  assertThrows(ConstraintViolationException.class, () -> {
      serviceUnderTest.update(result);
    });

    String expectedMessage = "must not be null";
    String actualMessage = exception.getMessage();

    assertTrue(actualMessage.contains(expectedMessage));
  }

  @Test
  public void validationGroups_DepartmentWithLongNameOnCreate_ThrowsException() {
    Department result = createLongNameDepartment();
    ConstraintViolationException exception = assertThrows(ConstraintViolationException.class, () -> {
      serviceUnderTest.create(result);
    });
    String expectedMessage = "size must be between 1 and 50";
    String actualMessage = exception.getMessage();

    assertTrue(actualMessage.contains(expectedMessage));
  }

  @Test
  public void validate_InvalidEntity_ThrowsValidationException() {
    Department d = new Department(); // not using the factory to get an empty object
    // should be a business rule validation but for testing we are using a default validator
    Validator defaultValidator = new SpringValidatorAdapter(Validation.buildDefaultValidatorFactory().getValidator());
    assertThrows(ValidationException.class, () -> serviceUnderTest.validate(d, UUID.randomUUID().toString(),
        defaultValidator));
  }

  private static Department createLongNameDepartment() {
    return Department
      .builder()
      .name(RandomStringUtils.random(51))
      .location(RandomStringUtils.random(5))
      .build();
  }

  public static class DinaServiceTestImplementation extends DefaultDinaService<Department> {

    @Inject
    private BaseDAO baseDAO;

    public DinaServiceTestImplementation(@NonNull BaseDAO baseDAO) {
      super(baseDAO);
    }

    @Override
    protected void preCreate(Department entity) {
      entity.setUuid(UUID.randomUUID());
    }

    @Override
    protected void preUpdate(Department entity) {
      DepartmentType type = DepartmentType.builder().name("name").build();
      baseDAO.create(type);
      entity.setDepartmentType(type);
    }

    @Override
    protected void preDelete(Department entity) {
      entity.setName(null);
      entity.setLocation(null);
    }

  }

}
