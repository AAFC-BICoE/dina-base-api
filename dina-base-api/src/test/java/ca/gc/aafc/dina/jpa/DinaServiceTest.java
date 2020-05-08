package ca.gc.aafc.dina.jpa;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
    Department result = createDepartment();
    assertNull(result.getId());
    serviceUnderTest.create(result);
    assertNotNull(result.getId());
  }

  private static Department createDepartment() {
    return Department
      .builder()
      .name(RandomStringUtils.random(5))
      .location(RandomStringUtils.random(5))
      .build();
  }
}
