package ca.gc.aafc.dina.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.UUID;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import ca.gc.aafc.dina.TestConfiguration;
import ca.gc.aafc.dina.dto.EmployeeDto;
import ca.gc.aafc.dina.dto.PersonDTO;
import ca.gc.aafc.dina.entity.Employee;
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
    EmployeeDto employeeRelation = persistEmployee();

    PersonDTO dto = createPersonDto();
    dto.setEmployee(employeeRelation);

    dinaRepository.create(dto);

    Person result = baseDAO.findOneByNaturalId(dto.getUuid(), Person.class);
    assertNotNull(result);
    assertEquals(dto.getUuid(), result.getUuid());
    assertEquals(dto.getName(), result.getName());
    assertEquals(employeeRelation.getId(), result.getEmployee().getId());
  }

  private PersonDTO createPersonDto() {
    return PersonDTO.builder().uuid(UUID.randomUUID()).name(RandomStringUtils.random(4)).build();
  }

  private EmployeeDto persistEmployee() {
    Employee emp = Employee.builder().name("name").job("").build();
    baseDAO.create(emp);
    return EmployeeDto.builder().id(emp.getId()).name(emp.getName()).job(emp.getJob()).build();
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
