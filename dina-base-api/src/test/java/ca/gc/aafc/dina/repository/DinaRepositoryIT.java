package ca.gc.aafc.dina.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.UUID;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import ca.gc.aafc.dina.TestConfiguration;
import ca.gc.aafc.dina.dto.DepartmentDto;
import ca.gc.aafc.dina.dto.PersonDTO;
import ca.gc.aafc.dina.entity.Department;
import ca.gc.aafc.dina.entity.Person;
import ca.gc.aafc.dina.jpa.BaseDAO;
import ca.gc.aafc.dina.jpa.DinaService;
import lombok.NonNull;

@Transactional
@SpringBootTest(classes = TestConfiguration.class)
public class DinaRepositoryIT {

  @Inject
  private DinaRepository<PersonDTO, Person> dinaRepository;

  @Inject
  private BaseDAO baseDAO;

  @Test
  public void create_ValidResource_ResourceCreated() {
    DepartmentDto departRelation = persistDepartment();

    PersonDTO dto = createPersonDto();
    dto.setDepartment(departRelation);

    dinaRepository.create(dto);

    Person result = baseDAO.findOneByNaturalId(dto.getUuid(), Person.class);
    assertNotNull(result);
    assertEquals(dto.getUuid(), result.getUuid());
    assertEquals(dto.getName(), result.getName());
    assertEquals(departRelation.getUuid(), result.getDepartment().getUuid());
  }

  private PersonDTO createPersonDto() {
    return PersonDTO.builder().uuid(UUID.randomUUID()).name(RandomStringUtils.random(4)).build();
  }

  private DepartmentDto persistDepartment() {
    Department depart = Department.builder()
      .uuid(UUID.randomUUID())
      .name("name")
      .location("location")
      .build();
    baseDAO.create(depart);
    return DepartmentDto.builder()
      .uuid(depart.getUuid())
      .name(depart.getName())
      .location(depart.getLocation())
      .build();
  }

  public static class DinaPersonService extends DinaService<Person> {

    public DinaPersonService(@NonNull BaseDAO baseDAO) {
      super(baseDAO);
    }

    @Override
    protected Person preCreate(Person entity) {
      return null;
    }

    @Override
    protected Person preUpdate(Person entity) {
      return null;
    }

    @Override
    protected void preDelete(Person entity) {
    }

  }
}
