package ca.gc.aafc.dina.jpa;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.UUID;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import ca.gc.aafc.dina.TestConfiguration;
import ca.gc.aafc.dina.entity.Department;

@Transactional
@SpringBootTest(classes = TestConfiguration.class)
public class DinaServiceTest {

  @Inject
  private DinaService<Department> serviceUnderTest;

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

  private Department persistDepartment() {
    Department result = createDepartment();
    assertNull(result.getId());
    serviceUnderTest.create(result);
    return result;
  }

  private static Department createDepartment() {
    return Department
      .builder()
      .uuid(UUID.randomUUID())
      .name(RandomStringUtils.random(5))
      .location(RandomStringUtils.random(5))
      .build();
  }
}
