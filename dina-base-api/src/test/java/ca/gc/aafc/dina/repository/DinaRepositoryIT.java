package ca.gc.aafc.dina.repository;

import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.hamcrest.core.Is;
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
import io.crnk.core.queryspec.Direction;
import io.crnk.core.queryspec.FilterOperator;
import io.crnk.core.queryspec.IncludeRelationSpec;
import io.crnk.core.queryspec.PathSpec;
import io.crnk.core.queryspec.QuerySpec;
import io.crnk.core.queryspec.SortSpec;
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
    PersonDTO dto = createPersonDto();
    dinaRepository.create(dto);

    Person result = baseDAO.findOneByNaturalId(dto.getUuid(), Person.class);
    assertNotNull(result);
    assertEqualsPersonDtoAndEntity(dto, result, true);
  }

  @Test
  public void findOne_ResourceAndRelations_FindsResourceAndRelations() {
    PersonDTO dto = createPersonDto();
    dinaRepository.create(dto);

    QuerySpec querySpec = new QuerySpec(PersonDTO.class);
    querySpec.setIncludedRelations(
      Arrays.asList("department", "departments").stream()
        .map(Arrays::asList)
        .map(IncludeRelationSpec::new)
        .collect(Collectors.toList()));

    PersonDTO result = dinaRepository.findOne(dto.getUuid(), querySpec);
    assertEqualsPersonDtos(dto, result, true);
  }

  // Add find one test for 404 not found

  @Test
  public void findAll_NoFilters_FindsAll() {
    Map<UUID, PersonDTO> expectedPersons = new HashMap<>();

    for (int i = 0; i < 10; i++) {
      PersonDTO dto = createPersonDto();
      dinaRepository.create(dto);
      expectedPersons.put(dto.getUuid(), dto);
    }

    List<PersonDTO> result = dinaRepository.findAll(null, new QuerySpec(PersonDTO.class));

    assertEquals(expectedPersons.size(), result.size());
    for (PersonDTO resultElement : result) {
      PersonDTO expectedDto = expectedPersons.get(resultElement.getUuid());
      assertEqualsPersonDtos(expectedDto, resultElement, false);
    }

  }

  @Test
  public void findAll_FilterByIds_FindsById() {
    List<Serializable> idList = new ArrayList<>();

    for (int i = 0; i < 10; i++) {
      PersonDTO dto = createPersonDto();
      dinaRepository.create(dto);
      idList.add(dto.getUuid());
      // Persist extra Person NOT IN idList
      PersonDTO extra = createPersonDto();
      dinaRepository.create(extra);
    }

    List<PersonDTO> resultList = dinaRepository.findAll(idList, new QuerySpec(PersonDTO.class));

    assertEquals(idList.size(), resultList.size());
    resultList.forEach(result -> assertTrue(idList.contains(result.getUuid())));
  }

  @Test
  public void findAll_FilterOnFieldEquals_FiltersOnField() {
    String expectedName = RandomStringUtils.random(4);
    int expectedNumberOfResults = 10;

    for (int i = 0; i < expectedNumberOfResults; i++) {
      PersonDTO dto = createPersonDto();
      dto.setName(expectedName);
      dinaRepository.create(dto);
      // Persist extra Person with different name
      PersonDTO extra = createPersonDto();
      dinaRepository.create(extra);
    }

    QuerySpec querySpec = new QuerySpec(PersonDTO.class);
    querySpec.addFilter(PathSpec.of("name").filter(FilterOperator.EQ, expectedName));

    List<PersonDTO> resultList = dinaRepository.findAll(null, querySpec);
    assertEquals(expectedNumberOfResults, resultList.size());
    resultList.forEach(result -> assertEquals(expectedName, result.getName()));
  }

  @Test
  public void findAll_FilterOnNestedFieldEquals_FiltersOnNestedField() {
    PersonDTO expectedDto = createPersonDto();
    dinaRepository.create(expectedDto);

    for (int i = 0; i < 10; i++) {
      PersonDTO toPersist = createPersonDto();
      toPersist.setDepartment(null);
      dinaRepository.create(toPersist);
    }

    QuerySpec querySpec = new QuerySpec(PersonDTO.class);
    querySpec.addFilter(
      PathSpec.of("department", "uuid").filter(
        FilterOperator.EQ,
        singleRelationUnderTest.getUuid()));

    List<PersonDTO> resultList = dinaRepository.findAll(null, querySpec);
    assertEquals(1, resultList.size());
    assertEquals(singleRelationUnderTest.getUuid(), resultList.get(0).getDepartment().getUuid());
  }

  @Test
  public void findAll_SortingByName_ReturnsSorted() {
    List<String> names = Arrays.asList("a", "b", "c", "d");
    List<String> shuffledNames = Arrays.asList("b", "a", "d", "c");

    for (String name : shuffledNames) {
      PersonDTO toPersist = createPersonDto();
      toPersist.setName(name);
      dinaRepository.create(toPersist);
    }

    QuerySpec querySpec = new QuerySpec(PersonDTO.class);
    querySpec.setSort(Arrays.asList(new SortSpec(Arrays.asList("name"), Direction.ASC)));

    List<PersonDTO> resultList = dinaRepository.findAll(null, querySpec);
    for (int i = 0; i < names.size(); i++) {
      assertEquals(names.get(i), resultList.get(i).getName());
    }
  }

  @Test
  public void findAll_whenPageLimitIsSet_pageSizeIsLimited() {
    long pageLimit = 10;

    for (int i = 0; i < pageLimit * 2; i++) {
      PersonDTO dto = createPersonDto();
      dinaRepository.create(dto);
    }

    QuerySpec querySpec = new QuerySpec(PersonDTO.class);
    querySpec.setLimit(pageLimit);

    List<PersonDTO> result = dinaRepository.findAll(null, querySpec);
    assertEquals(pageLimit, result.size());
  }

  @Test
  public void findAll_whenPageOffsetIsSet_pageStartsAfterOffset() {
    long offset = 2;
    List<PersonDTO> dtos = new ArrayList<>();

    for (int i = 0; i < 10; i++) {
      PersonDTO dto = createPersonDto();
      dinaRepository.create(dto);
      dtos.add(dto);
    }

    QuerySpec querySpec = new QuerySpec(PersonDTO.class);
    querySpec.setOffset(offset);
    List<PersonDTO> result = dinaRepository.findAll(null, querySpec);

    List<PersonDTO> expectedDtos = dtos.subList((int) offset, dtos.size());
    for (int i = 0; i < expectedDtos.size(); i++) {
      assertEqualsPersonDtos(expectedDtos.get(i), result.get(i), false);
    }
  }

  @Test
  public void findAll_NothingPersisted_ReturnsEmpty() {
    List<PersonDTO> result = dinaRepository.findAll(null, new QuerySpec(PersonDTO.class));
    assertEquals(0, result.size());
  }

  private void assertEqualsPersonDtos(PersonDTO dto, PersonDTO result, boolean testRelations) {
    assertEquals(dto.getUuid(), result.getUuid());
    assertEquals(dto.getName(), result.getName());
    assertArrayEquals(dto.getNickNames(), result.getNickNames());
    if (testRelations) {
      assertEquals(singleRelationUnderTest.getUuid(), result.getDepartment().getUuid());
      for (int i = 0; i < collectionRelationUnderTest.size(); i++) {
        assertEquals(
          collectionRelationUnderTest.get(i).getUuid(),
          result.getDepartments().get(i).getUuid()
        );
      }
    }
  }

  private void assertEqualsPersonDtoAndEntity(PersonDTO dto, Person entity, boolean testRelations) {
    assertEquals(dto.getUuid(), entity.getUuid());
    assertEquals(dto.getName(), entity.getName());
    assertArrayEquals(dto.getNickNames(), entity.getNickNames());
    if (testRelations) {
      assertTrue(EqualsBuilder.reflectionEquals(singleRelationUnderTest, entity.getDepartment()));
      assertThat(collectionRelationUnderTest, Is.is(entity.getDepartments()));
    }
  }

  private PersonDTO createPersonDto() {
    DepartmentDto singleRelationDto = DepartmentDto.builder()
        .uuid(singleRelationUnderTest.getUuid())
        .build();
    List<DepartmentDto> collectionRelationDtos = collectionRelationUnderTest.stream()
        .map(c -> DepartmentDto.builder().uuid(c.getUuid()).build())
        .collect(Collectors.toList());
    return PersonDTO.builder()
        .uuid(UUID.randomUUID())
        .department(singleRelationDto)
        .departments(collectionRelationDtos)
        .nickNames(Arrays.asList("d", "z", "q").toArray(new String[0]))
        .name(RandomStringUtils.random(4)).build();
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
