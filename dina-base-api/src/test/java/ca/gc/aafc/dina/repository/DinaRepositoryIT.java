package ca.gc.aafc.dina.repository;

import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import io.crnk.core.exception.ResourceNotFoundException;
import io.crnk.core.queryspec.Direction;
import io.crnk.core.queryspec.FilterOperator;
import io.crnk.core.queryspec.IncludeRelationSpec;
import io.crnk.core.queryspec.PathSpec;
import io.crnk.core.queryspec.QuerySpec;
import io.crnk.core.queryspec.SortSpec;
import io.crnk.core.resource.list.ResourceList;
import io.crnk.core.resource.meta.PagedMetaInformation;
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
    PersonDTO dto = persistPerson();

    Person result = baseDAO.findOneByNaturalId(dto.getUuid(), Person.class);
    assertNotNull(result);
    assertEqualsPersonDtoAndEntity(dto, result, singleRelationUnderTest, collectionRelationUnderTest);
  }

  @Test
  public void findOne_ResourceAndRelations_FindsResourceAndRelations() {
    PersonDTO dto = persistPerson();

    QuerySpec querySpec = new QuerySpec(PersonDTO.class);
    querySpec.setIncludedRelations(createIncludeRelationSpecs("department", "departments"));

    PersonDTO result = dinaRepository.findOne(dto.getUuid(), querySpec);
    assertEqualsPersonDtos(dto, result, true);
  }

  @Test
  public void findOne_ExcludeRelations_RelationsExcluded() {
    PersonDTO dto = persistPerson();

    PersonDTO result = dinaRepository.findOne(dto.getUuid(), new QuerySpec(PersonDTO.class));
    assertEqualsPersonDtos(dto, result, false);
    assertNull(result.getDepartment());
    assertNull(result.getDepartments());
  }

  @Test
  public void findOne_NoResourceFound_ThrowsResourceNotFoundException() {
    assertThrows(
      ResourceNotFoundException.class,
      ()-> dinaRepository.findOne(UUID.randomUUID(), new QuerySpec(PersonDTO.class))
    );
  }

  @Test
  public void findAll_NoFilters_FindsAllAndExcludesRelationships() {
    Map<UUID, PersonDTO> expectedPersons = new HashMap<>();

    for (int i = 0; i < 10; i++) {
      PersonDTO dto = persistPerson();
      expectedPersons.put(dto.getUuid(), dto);
    }

    List<PersonDTO> result = dinaRepository.findAll(null, new QuerySpec(PersonDTO.class));

    assertEquals(expectedPersons.size(), result.size());
    for (PersonDTO resultElement : result) {
      PersonDTO expectedDto = expectedPersons.get(resultElement.getUuid());
      assertEqualsPersonDtos(expectedDto, resultElement, false);
      assertNull(resultElement.getDepartment());
      assertNull(resultElement.getDepartments());
    }
  }

  @Test
  public void findAll_ResourceAndRelations_FindsResourceAndRelations() {
    Map<UUID, PersonDTO> expectedPersons = new HashMap<>();

    for (int i = 0; i < 10; i++) {
      PersonDTO dto = persistPerson();
      expectedPersons.put(dto.getUuid(), dto);
    }

    QuerySpec querySpec = new QuerySpec(PersonDTO.class);
    querySpec.setIncludedRelations(createIncludeRelationSpecs("department", "departments"));

    List<PersonDTO> result = dinaRepository.findAll(null, querySpec);

    assertEquals(expectedPersons.size(), result.size());
    for (PersonDTO resultElement : result) {
      PersonDTO expectedDto = expectedPersons.get(resultElement.getUuid());
      assertEqualsPersonDtos(expectedDto, resultElement, true);
    }
  }

  @Test
  public void findAll_FilterByIds_FindsById() {
    List<Serializable> idList = new ArrayList<>();

    for (int i = 0; i < 10; i++) {
      PersonDTO dto = persistPerson();
      idList.add(dto.getUuid());
      // Persist extra person not in list
      persistPerson();
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
      // Persist extra person with different name
      persistPerson();
    }

    QuerySpec querySpec = new QuerySpec(PersonDTO.class);
    querySpec.addFilter(PathSpec.of("name").filter(FilterOperator.EQ, expectedName));

    List<PersonDTO> resultList = dinaRepository.findAll(null, querySpec);
    assertEquals(expectedNumberOfResults, resultList.size());
    resultList.forEach(result -> assertEquals(expectedName, result.getName()));
  }

  @Test
  public void findAll_FilterOnNestedFieldEquals_FiltersOnNestedField() {
    PersonDTO expected = persistPerson();

    // Persist extra people with no department
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
    assertEqualsPersonDtos(expected, resultList.get(0), false);
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
      persistPerson();
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
      PersonDTO dto = persistPerson();
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
  public void findAll_FilterByIds_ReturnsTotalCount() {
    List<Serializable> idList = new ArrayList<>();

    for (int i = 0; i < 10; i++) {
      PersonDTO dto = persistPerson();
      idList.add(dto.getUuid());
      // Persist extra person not in list
      persistPerson();
    }

    ResourceList<PersonDTO> resultList = dinaRepository.findAll(idList, new QuerySpec(PersonDTO.class));
    PagedMetaInformation metadata = (PagedMetaInformation) resultList.getMeta();

    assertEquals(idList.size(), metadata.getTotalResourceCount());
  }

  @Test
  public void findAll_whenPageLimitIsSet_ReturnsTotalCount() {
    long pageLimit = 10;
    long totalResouceCount = pageLimit * 2;

    for (int i = 0; i < totalResouceCount; i++) {
      persistPerson();
    }

    QuerySpec querySpec = new QuerySpec(PersonDTO.class);
    querySpec.setLimit(pageLimit);

    ResourceList<PersonDTO> result = dinaRepository.findAll(null, querySpec);
    PagedMetaInformation metadata = (PagedMetaInformation) result.getMeta();
    assertEquals(totalResouceCount, metadata.getTotalResourceCount());
  }

  @Test
  public void findAll_NothingPersisted_ReturnsEmpty() {
    List<PersonDTO> result = dinaRepository.findAll(null, new QuerySpec(PersonDTO.class));
    assertEquals(0, result.size());
  }

  @Test
  public void save_UpdateAllFields_AllFieldsUpdated() {
    Department expectedDept = persistDepartment();
    DepartmentDto newDepart = DepartmentDto.builder().uuid(expectedDept.getUuid()).build();

    List<Department> expectedDepts = persistDepartments();
    List<DepartmentDto> newDepartments = expectedDepts.stream()
      .map(d -> DepartmentDto.builder().uuid(d.getUuid()).build())
      .collect(Collectors.toList());

    String expectedName = "new name";
    String[] expectedNickNames = Arrays.asList("new", "nick", "names").toArray(new String[0]);

    PersonDTO dto = persistPerson();

    dto.setName(expectedName);
    dto.setNickNames(expectedNickNames);
    dto.setDepartments(newDepartments);
    dto.setDepartment(newDepart);

    dinaRepository.save(dto);

    Person result = baseDAO.findOneByNaturalId(dto.getUuid(), Person.class);
    assertEqualsPersonDtoAndEntity(dto, result, expectedDept, expectedDepts);
  }

  @Test
  public void save_NullAllFields_AllFieldsNulled() {
    PersonDTO dto = persistPerson();

    dto.setName(null);
    dto.setNickNames(null);
    dto.setDepartments(null);
    dto.setDepartment(null);

    dinaRepository.save(dto);

    Person result = baseDAO.findOneByNaturalId(dto.getUuid(), Person.class);
    assertNull(result.getName());
    assertNull(result.getNickNames());
    assertNull(result.getDepartment());
    assertNull(result.getDepartments());
  }

  @Test
  public void save_NoResourceFound_ThrowsResourceNotFoundException() {
    assertThrows(ResourceNotFoundException.class, () -> dinaRepository.save(createPersonDto()));
  }

  @Test
  public void delete_ValidResource_ResourceRemoved() {
    PersonDTO dto = persistPerson();

    assertNotNull(baseDAO.findOneByNaturalId(dto.getUuid(), Person.class));

    dinaRepository.delete(dto.getUuid());
    assertNull(baseDAO.findOneByNaturalId(dto.getUuid(), Person.class));
  }

  @Test
  public void delete_NoResourceFound_ThrowsResourceNotFoundException() {
    assertThrows(ResourceNotFoundException.class, () -> dinaRepository.delete(UUID.randomUUID()));
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

  private static void assertEqualsPersonDtoAndEntity(
    PersonDTO dto,
    Person entity,
    Department expectedDepartment,
    List<Department> expectedDepartments
  ) {
    assertEquals(dto.getUuid(), entity.getUuid());
    assertEquals(dto.getName(), entity.getName());
    assertArrayEquals(dto.getNickNames(), entity.getNickNames());
    assertTrue(EqualsBuilder.reflectionEquals(expectedDepartment, entity.getDepartment()));
    assertThat(expectedDepartments, Is.is(entity.getDepartments()));
  }

  private PersonDTO persistPerson() {
    PersonDTO dto = createPersonDto();
    return dinaRepository.create(dto);
  }

  private PersonDTO createPersonDto() {
    DepartmentDto singleRelationDto = DepartmentDto.builder()
        .uuid(singleRelationUnderTest.getUuid())
        .build();
    List<DepartmentDto> collectionRelationDtos = collectionRelationUnderTest.stream()
        .map(c -> DepartmentDto.builder().uuid(c.getUuid()).build())
        .collect(Collectors.toList());
    return PersonDTO.builder()
        .department(singleRelationDto)
        .departments(collectionRelationDtos)
        .nickNames(Arrays.asList("d", "z", "q").toArray(new String[0]))
        .name(RandomStringUtils.random(4)).build();
  }

  private static Department createDepartment(String name, String Location) {
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

  private static List<IncludeRelationSpec> createIncludeRelationSpecs(String... args) {
    return Arrays.asList(args).stream()
      .map(Arrays::asList)
      .map(IncludeRelationSpec::new)
      .collect(Collectors.toList());
  }

  public static class DinaPersonService extends DinaService<Person> {

    public DinaPersonService(@NonNull BaseDAO baseDAO) {
      super(baseDAO);
    }

    @Override
    protected Person preCreate(Person entity) {
      entity.setUuid(UUID.randomUUID());
      return entity;
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
