package ca.gc.aafc.dina.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
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

  private Department singleRelationUnderTest;
  private List<Department> collectionRelationUnderTest;

  @BeforeEach
  public void setup() {
    singleRelationUnderTest =  persistDepartment();
    collectionRelationUnderTest = persistDepartments();
  }

  @Test
  public void create_ValidResource_ResourceCreated() {
    DepartmentDto singleRelationDto = DepartmentDto.builder()
      .uuid(singleRelationUnderTest.getUuid())
      .build();
    List<DepartmentDto> collectionRelationDtos = collectionRelationUnderTest.stream()
      .map(c -> DepartmentDto.builder().uuid(c.getUuid()).build())
      .collect(Collectors.toList());

    PersonDTO dto = createPersonDto();
    dto.setDepartment(singleRelationDto);
    dto.setDepartments(collectionRelationDtos);

    dinaRepository.create(dto);

    Person result = baseDAO.findOneByNaturalId(dto.getUuid(), Person.class);
    assertNotNull(result);
    assertEquals(dto.getUuid(), result.getUuid());
    assertEquals(dto.getName(), result.getName());

    assertEquals(singleRelationUnderTest.getUuid(), result.getDepartment().getUuid());
    assertEquals(singleRelationUnderTest.getName(), result.getDepartment().getName());
    assertEquals(singleRelationUnderTest.getLocation(), result.getDepartment().getLocation());

    assertCollectionRelationEquals(collectionRelationUnderTest, result.getDepartments());
  }

  private static void assertCollectionRelationEquals(List<Department> expected, List<Department> result) {
    assertEquals(expected.size(), result.size());
    expected.stream()
      .collect(Collectors.toMap(Department::getUuid, Function.identity()))
      .entrySet()
      .forEach(
        e->{
          Department resultingDto = result.stream().filter(d->d.getUuid().equals(e.getKey())).findFirst().get();
          assertEquals(e.getValue().getUuid(), resultingDto.getUuid());
          assertEquals(e.getValue().getName(), resultingDto.getName());
          assertEquals(e.getValue().getLocation(), resultingDto.getLocation());
        });
  }

  private PersonDTO createPersonDto() {
    return PersonDTO.builder().uuid(UUID.randomUUID()).name(RandomStringUtils.random(4)).build();
  }

  private Department createDepartment(String name, String Location) {
    Department depart = Department.builder()
      .uuid(UUID.randomUUID())
      .name(name)
      .location(Location)
      .build();
    return depart;
  }

  private Department persistDepartment() {
    Department depart = createDepartment(RandomStringUtils.random(4), RandomStringUtils.random(4));
    baseDAO.create(depart);
    return depart;
  }

  private List<Department> persistDepartments() {
    List<Department> departments = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      departments.add(persistDepartment());
    }
    return departments;
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
