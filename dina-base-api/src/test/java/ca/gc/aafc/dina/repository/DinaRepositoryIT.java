package ca.gc.aafc.dina.repository;

import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.dto.DepartmentDto;
import ca.gc.aafc.dina.dto.PersonDTO;
import ca.gc.aafc.dina.entity.Department;
import ca.gc.aafc.dina.entity.Person;
import ca.gc.aafc.dina.jpa.BaseDAO;
import ca.gc.aafc.dina.repository.meta.DinaMetaInfo;
import ca.gc.aafc.dina.service.DefaultDinaService;
import ca.gc.aafc.dina.testsupport.PostgresTestContainerInitializer;
import io.crnk.core.exception.ResourceNotFoundException;
import io.crnk.core.queryspec.Direction;
import io.crnk.core.queryspec.FilterOperator;
import io.crnk.core.queryspec.IncludeRelationSpec;
import io.crnk.core.queryspec.PathSpec;
import io.crnk.core.queryspec.QuerySpec;
import io.crnk.core.queryspec.SortSpec;
import io.crnk.core.resource.list.ResourceList;
import io.crnk.core.resource.meta.PagedMetaInformation;
import java.util.Comparator;
import lombok.NonNull;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.hamcrest.core.Is;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.validation.SmartValidator;

import javax.inject.Inject;
import javax.transaction.Transactional;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@Transactional
@SpringBootTest(classes = TestDinaBaseApp.class)
@ContextConfiguration(initializers = { PostgresTestContainerInitializer.class })
public class DinaRepositoryIT {

  @Inject
  private DinaRepository<PersonDTO, Person> dinaRepository;

  @Inject
  private DinaRepository<DepartmentDto, Department> departmentRepository;

  @Inject
  private BaseDAO baseDAO;

  private Department singleRelationUnderTest;
  private List<Department> collectionRelationUnderTest;

  @BeforeEach
  public void setup() {
    singleRelationUnderTest = persistDepartment();
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
  public void findOne_NoResourceFound_ThrowsResourceNotFoundException() {
    assertThrows(
      ResourceNotFoundException.class,
      () -> dinaRepository.findOne(UUID.randomUUID(), new QuerySpec(PersonDTO.class))
    );
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
  public void findAll_NestedRelations_FindsResourceAndRelations() {
    // Create the owner (personDto)
    PersonDTO ownerDto = PersonDTO.builder()
        .uuid(UUID.randomUUID())
        .name("Person_" + RandomStringUtils.randomAlphabetic(5))
        .build();
    ownerDto = dinaRepository.create(ownerDto);

    // Create the department and connect the department owner to the person created above.
    DepartmentDto departmentDto = DepartmentDto.builder()
        .uuid(UUID.randomUUID())
        .name("Department_" + RandomStringUtils.randomAlphabetic(5))
        .location("Ottawa, Ontario, Canada")
        .departmentOwner(ownerDto)
        .build();
    departmentDto = departmentRepository.create(departmentDto);

    // Separate person that will be connected to the department.
    PersonDTO personDto = PersonDTO.builder()
        .uuid(UUID.randomUUID())
        .name("John Doe")
        .department(departmentDto)
        .build();
    personDto = dinaRepository.create(personDto);

    // Create the query with the nested include (personDto -> department -> ownerDto).
    QuerySpec querySpec = new QuerySpec(PersonDTO.class);
    querySpec.setIncludedRelations(List.of(
      new IncludeRelationSpec(List.of("department", "departmentOwner"))
    ));

    // Starting the query from the personDto, try to retrieve the ownerDto using nested includes.
    List<PersonDTO> result = dinaRepository.findAll(List.of(personDto.getUuid()), querySpec);
    assertEquals(ownerDto.getName(), result.get(0).getDepartment().getDepartmentOwner().getName());
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
  public void findAll_FilterWithRSQL_FiltersOnRSQL() {
    String expectedName = RandomStringUtils.randomAlphabetic(4);
    int expectedNumberOfResults = 10;

    for (int i = 0; i < expectedNumberOfResults; i++) {
      PersonDTO dto = createPersonDto();
      dto.setName(expectedName);
      dinaRepository.create(dto);
      // Persist extra person with different name
      persistPerson();
    }

    QuerySpec querySpec = new QuerySpec(PersonDTO.class);
    querySpec.addFilter(PathSpec.of("rsql").filter(FilterOperator.EQ, "name==" + expectedName));

    List<PersonDTO> resultList = dinaRepository.findAll(null, querySpec);
    assertEquals(expectedNumberOfResults, resultList.size());
    resultList.forEach(result -> assertEquals(expectedName, result.getName()));
  }

  @Test
  public void findAll_FilterOnFieldEquals_FiltersOnField() {
    String expectedName = RandomStringUtils.randomAlphabetic(4);
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
  public void findAll_FilterOnCustomField() {
    List<PersonDTO> persited = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      persited.add(persistPerson());
    }

    QuerySpec querySpec = new QuerySpec(PersonDTO.class);
    PersonDTO expected = persited.get(0);
    querySpec.addFilter(
      PathSpec.of("customField").filter(FilterOperator.EQ, expected.getCustomField()));

    List<PersonDTO> resultList = dinaRepository.findAll(null, querySpec);
    assertEquals(1, resultList.size());
    assertEquals(expected.getUuid(), resultList.get(0).getUuid());
  }

  @Test
  public void findAll_FilterOnNestedCustomField() {
    Department depart = persistDepartment();
    PersonDTO expected = createPersonDto();
    expected.setDepartment(DepartmentDto.builder().uuid(depart.getUuid()).build());
    expected = dinaRepository.create(expected);

    // Persist extra people with no department
    for (int i = 0; i < 10; i++) {
      PersonDTO toPersist = createPersonDto();
      toPersist.setDepartment(null);
      dinaRepository.create(toPersist);
    }

    QuerySpec querySpec = new QuerySpec(PersonDTO.class);

    querySpec.addFilter(PathSpec.of("department", "derivedFromLocation")
      .filter(FilterOperator.EQ, depart.getLocation()));

    List<PersonDTO> resultList = dinaRepository.findAll(null, querySpec);
    assertEquals(1, resultList.size());
    assertEquals(expected.getUuid(), resultList.get(0).getUuid());
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
    querySpec.setSort(Collections.singletonList(
        new SortSpec(Collections.singletonList("name"), Direction.ASC)));

    List<PersonDTO> resultList = dinaRepository.findAll(null, querySpec);
    for (int i = 0; i < names.size(); i++) {
      assertEquals(names.get(i), resultList.get(i).getName());
    }
  }

  @Test
  public void findAll_SortingByName_ReturnsSortedCaseSensitiveOrNot() {

    List<String> shuffledNames = Arrays.asList("b", "a", "d", "C");
    List<Integer> matchingRooms = Arrays.asList(6, 2, 1, 11);

    List<String> namesCaseInsensitive = Arrays.asList("a", "b", "C", "d");
    List<String> namesCaseSensitive = Arrays.asList("C", "a", "b", "d");
    List<String> byRoom = Arrays.asList("d", "a", "b", "C");

    for (int i=0; i < shuffledNames.size(); i++) {
      PersonDTO toPersist = createPersonDto();
      toPersist.setName(shuffledNames.get(i));
      toPersist.setRoom(matchingRooms.get(i));
      dinaRepository.create(toPersist);
    }

    QuerySpec querySpec = new QuerySpec(PersonDTO.class);
    querySpec.setSort(Collections.singletonList(
        new SortSpec(Collections.singletonList("name"), Direction.ASC)));

    //case insensitive by default
    List<PersonDTO> resultList = dinaRepository.findAll(null, querySpec);
    for (int i = 0; i < namesCaseInsensitive.size(); i++) {
      assertEquals(namesCaseInsensitive.get(i), resultList.get(i).getName());
    }

    //test case sensitive
    dinaRepository.setCaseSensitiveOrderBy(true);
    resultList = dinaRepository.findAll(null, querySpec);
    for (int i = 0; i < namesCaseSensitive.size(); i++) {
      assertEquals(namesCaseSensitive.get(i), resultList.get(i).getName());
    }

    // revert to default
    dinaRepository.setCaseSensitiveOrderBy(false);

    // sorting on non text field should have no effect
    querySpec.setSort(Collections.singletonList(
        new SortSpec(Collections.singletonList("room"), Direction.ASC)));
    resultList = dinaRepository.findAll(null, querySpec);
    for (int i = 0; i < namesCaseSensitive.size(); i++) {
      assertEquals(byRoom.get(i), resultList.get(i).getName());
    }
  }

  @Test
  public void findAll_SortingByNestedProperty_ReturnsSorted() {
    List<String> names = Arrays.asList("a", "b", "c", "d");
    List<String> shuffledNames = Arrays.asList("b", "a", "d", "c");

    for (String name : shuffledNames) {
      Department depart = createDepartment(name, "location");
      baseDAO.create(depart);
      PersonDTO toPersist = createPersonDto();
      toPersist.setDepartment(DepartmentDto.builder().uuid(depart.getUuid()).build());
      dinaRepository.create(toPersist);
    }

    QuerySpec querySpec = new QuerySpec(PersonDTO.class);
    querySpec.setIncludedRelations(createIncludeRelationSpecs("department"));
    querySpec.setSort(
        Collections.singletonList(new SortSpec(Arrays.asList("department", "name"), Direction.ASC)));

    List<PersonDTO> resultList = dinaRepository.findAll(null, querySpec);
    for (int i = 0; i < names.size(); i++) {
      assertEquals(names.get(i), resultList.get(i).getDepartment().getName());
    }
  }

  @Test
  public void findAll_SortingByNestedProperty_ReturnsResourcesWithNullProperty() {
    for (int i = 0; i < 3; i++) {
      PersonDTO noRelation = createPersonDto();
      noRelation.setDepartment(null);
      noRelation.setDepartmentsHeadBackup(null);
      dinaRepository.create(noRelation);
    }

    QuerySpec querySpec = new QuerySpec(PersonDTO.class);
    querySpec.setSort(Collections.singletonList(
      new SortSpec(Arrays.asList("department", "name"), Direction.ASC)));

    List<PersonDTO> resultList = dinaRepository.findAll(null, querySpec);
    Assertions.assertTrue(CollectionUtils.isNotEmpty(resultList), "no results were returned");
    resultList.forEach(result -> Assertions.assertNull(result.getDepartment()));
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

    QuerySpec querySpec = new QuerySpec(PersonDTO.class);
    List<PersonDTO> result = dinaRepository.findAll(null, querySpec);

    //cleanup old records
    for(PersonDTO oldRecord : result) {
      dinaRepository.delete(oldRecord.getUuid());
    }

    for (int i = 0; i < 10; i++) {
      PersonDTO dto = persistPerson();
      dtos.add(dto);
    }

    querySpec.setOffset(offset);
    querySpec.setSort(List.of(SortSpec.asc(List.of("name"))));
    result = dinaRepository.findAll(null, querySpec);
    dtos.sort(Comparator.comparing( p -> p.getName().toLowerCase()));

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
  /** Tests the fix from #20605 for an error on nested include relations. */
  public void findAll_whenNestedIncludeRequested_noErrorThrown() {
    QuerySpec querySpec = new QuerySpec(PersonDTO.class);
    querySpec.setIncludedRelations(
      List.of(
        new IncludeRelationSpec(List.of("department")),
        new IncludeRelationSpec(List.of("department", "departmentHead")),
        new IncludeRelationSpec(List.of("department", "departmentHead", "department"))
      ));

    dinaRepository.findAll(querySpec);
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
    dto.setDepartmentsHeadBackup(newDepartments);
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
    dto.setDepartmentsHeadBackup(null);
    dto.setDepartment(null);

    dinaRepository.save(dto);

    Person result = baseDAO.findOneByNaturalId(dto.getUuid(), Person.class);
    assertNull(result.getName());
    assertNull(result.getNickNames());
    assertNull(result.getDepartment());
    assertNull(result.getDepartmentsHeadBackup());
  }

  @Test
  public void save_NoResourceFound_ThrowsResourceNotFoundException() {
    assertThrows(ResourceNotFoundException.class, () -> dinaRepository.save(createPersonDto()));
  }

  @Test
  public void save_unsafePayload_ExceptionThrown() {
    PersonDTO personDTO = createPersonDto();
    personDTO.setName("abc<iframe src=javascript:alert(32311)>");
    assertThrows(IllegalArgumentException.class, () -> dinaRepository.create(personDTO));
  }

  @Test
  public void save_unsafePayloadInRelationship_NoExceptionThrown() {
    PersonDTO personDTO = createPersonDto();

    // The goal here is to bypass the repository layer to insert illegal content to make sure it won't break
    // the main resource when used as a relationship. It's the responsibility of the repository of the resource to
    // validate it<s own content but not the relationship.
    UUID depUUID = UUID.randomUUID();
    Department depDTO = Department.builder().uuid(depUUID).name("abc<iframe src=javascript:alert(32311)>").build();
    baseDAO.create(depDTO, true);
    DepartmentDto deptDTO = departmentRepository.findOne(depUUID, new QuerySpec(PersonDTO.class));

    personDTO.setDepartment(deptDTO);
    dinaRepository.create(personDTO);
  }

  @Test
  public void create_unsafeNestedPayload_ExceptionThrown() {
    Department.DepartmentDetails departmentDetails = new Department.DepartmentDetails("note<iframe src=javascript:alert(32311)>");
    DepartmentDto departmentDto = DepartmentDto.builder()
            .uuid(UUID.randomUUID())
            .departmentDetails(departmentDetails)
            .build();
    assertThrows(IllegalArgumentException.class, () -> departmentRepository.create(departmentDto));
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

  @Test
  public void getMetaInformation_whenBuildVersionIsSet_buildVersionIncludedInMeta() {
    DinaMetaInfo meta = dinaRepository.getMetaInformation(
      List.of(),
      new QuerySpec(PersonDTO.class),
      new DinaMetaInfo()
    );

    assertEquals("test-api-version", meta.getModuleVersion());
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
          result.getDepartmentsHeadBackup().get(i).getUuid()
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
    assertThat(expectedDepartments, Is.is(entity.getDepartmentsHeadBackup()));
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
      .departmentsHeadBackup(collectionRelationDtos)
      .nickNames(Arrays.asList("d", "z", "q").toArray(new String[0]))
      .name("DinaRepositoryIT_" + RandomStringUtils.randomAlphabetic(4))
      .group(RandomStringUtils.randomAlphabetic(4))
      .build();
  }

  private static Department createDepartment(String name, String Location) {
    return Department.builder()
      .uuid(UUID.randomUUID())
      .name(name)
      .location(Location)
      .build();
  }

  private Department persistDepartment() {
    Department depart = createDepartment(RandomStringUtils.randomAlphabetic(4), RandomStringUtils.randomAlphabetic(4));
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
    return Arrays.stream(args)
      .map(Arrays::asList)
      .map(IncludeRelationSpec::new)
      .collect(Collectors.toList());
  }

  public static class DinaPersonService extends DefaultDinaService<Person> {

    public DinaPersonService(@NonNull BaseDAO baseDAO, SmartValidator sv) {
      super(baseDAO, sv);
    }

    @Override
    protected void preCreate(Person entity) {
      entity.setUuid(UUID.randomUUID());
      entity.setGroup(standardizeGroupName(entity));
    }
  }
}
